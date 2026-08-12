## Context

See `proposal.md` — Why. This is a greenfield repository: no build files, no source, no existing specs.

Constraints that shape everything below:

- **Paper 26.2 requires Java 25**, which matches the requested toolchain. Velocity runs on the same JVM family, so a single Java 25 toolchain covers both artifacts.
- **The proxy is the only process that knows the full player roster.** No backend server can answer "is `Steve` online, and where?" on its own. Every cross-server decision must route through Velocity.
- **Plugin messaging rides player connections.** Velocity can only push a message to a backend server that has at least one player online (`RegisteredServer.sendPluginMessage`). An empty server is unreachable. This is the single hardest constraint in the design and drives the database-as-source-of-truth decision below.
- **No comments in Java source.** Every name must carry its own meaning, and anything that would have been a comment becomes either a method name, a record component name, or a line in a `.yml` file.
- **Immutable-first.** Records for data, `final` everywhere, no reassignment. Caches and pending-request maps are the only mutable state and are confined to named holder classes.

## Goals / Non-Goals

**Goals:**

- One shared, eventually-consistent report set observable from any backend server, converging within the reconciliation interval even after messaging gaps.
- A single round-trip user experience: file a report, or click once to end up standing next to the reported player on another server.
- A protocol that fails closed — unknown versions, malformed frames, and non-proxy senders change no state.
- All operator-visible behaviour reachable from `.yml` without touching code.

**Non-Goals:**

- **No in-game configuration reload.** The requested command surface is exactly four commands; a `reload` subcommand is not among them. Configuration is read at plugin enable. Adding reload later is additive and touches no spec.
- **No punishment integration.** `s-reports` records and routes reports; banning, muting, and evidence capture are out of scope.
- **No web panel, no Discord relay, no report chat log capture.**
- **No Redis or message broker.** Chosen transport is Velocity plugin messaging plus MySQL reconciliation, per the decision recorded with the user.
- **No Bedrock/Geyser-specific handling.**

## Decisions

### Module layout: three Gradle modules, one toolchain

```
s-reports/
  settings.gradle.kts
  build.gradle.kts                 <- toolchain 25, shared repos, checkstyle
  s-reports-common/                <- platform-free: domain, protocol, storage, config
  s-reports-paper/                 <- Paper 26.2 plugin, shades common
  s-reports-velocity/              <- Velocity plugin, shades common
```

`s-reports-common` depends on neither platform API. It holds the report records, the wire codec, the MySQL repository, the duration/config parsing, and the filtering logic — which is also where nearly all of the unit testing lives, since it runs without a server.

*Alternative considered:* a single-module plugin with a `platform` package and reflection-based dispatch. Rejected: the Paper and Velocity APIs have colliding type names (`Player`, `Server`, `Component` differ), and one module cannot compile against both without shadowing pain.

### Velocity is the routing authority; Paper owns the database writes

Velocity answers exactly three questions and nothing else: *which server is this player on*, *deliver this frame to every backend*, and *move this player to that server*. It deliberately holds no report business logic.

Paper performs validation, writes the report, and renders every user-facing message. This keeps the message catalogue in one file on one platform and keeps the proxy plugin thin enough to be obviously correct.

Both platforms open their own MySQL pool: Velocity for the roster-independent bookkeeping it needs, Paper for report writes, menu reads, lifetime counts, and reconciliation.

*Alternative considered:* Velocity as the sole database client, with backends hydrating purely over plugin messages. Rejected: a full sync frame would exceed the plugin message payload ceiling once report volume grows, forcing chunking and reassembly, and it would leave a backend with a stale view whenever the proxy restarts.

### Target resolution is a two-hop probe, not a proxy permission lookup

The exempt-target check must read the target's permissions. Checking them on Velocity would require every network to install a proxy-side permission provider and mirror the node there. Instead the probe travels to the server that already holds the target:

```
Paper A  --TargetResolveRequest{requestId, reporterId, targetName}-->  Velocity
Velocity  resolves targetName -> Player -> RegisteredServer
   |  not found  --TargetResolveResponse{NOT_FOUND}-->  Paper A
   +--TargetProbe{requestId, originServer, targetId}-->  Paper T
Paper T  --TargetProbeResult{requestId, originServer, targetId, targetName, exempt}-->  Velocity
Velocity  --TargetResolveResponse{requestId, targetId, targetName, server, exempt}-->  Paper A
```

Paper A holds the pending request in a map keyed by `requestId` with a configurable timeout; on timeout the reporter gets the resolve-timeout message and no report is written. Permissions are always read on the server where the player actually is, so `LuckPerms` on the backends is sufficient and no proxy-side permission provider is required.

*Alternative considered:* `player.hasPermission()` on Velocity. Rejected for the deployment burden above.

### Teleport is arm-then-transfer, never transfer-then-hope

```
Paper A  --TeleportRequest{staffId, reportId, targetId}-->  Velocity
Velocity  resolves target's current server
   |  offline        --TeleportDenied{TARGET_OFFLINE}-->     Paper A
   |  transfer fails --TeleportDenied{TRANSFER_FAILED}-->    Paper A
   |  same server    --TeleportGrant{local}-->               Paper A
   +--TeleportArm{staffId, targetId, reportId, expiresAt}--> Paper T
      then  player.createConnectionRequest(targetServer).connect()
```

`Paper T` records the pending teleport **before** the transfer is initiated, so the arm frame cannot lose the race against `PlayerJoinEvent`. On join, `Paper T` teleports only if the pending entry is unexpired *and* the target is still online on that server; otherwise it sends the target-moved or target-offline message. Pending entries expire on a configurable timeout (default `10s`) so a staff member who wanders back later is not teleported unexpectedly.

Deny reasons cross the wire as enum ordinals, not text — `Paper A` renders them from its own `messages.yml`.

### Database is the source of truth; messaging is an accelerator

Every state change is written to MySQL first and broadcast second. Backends reconcile their valid-report view from MySQL on plugin enable, on the first player joining an otherwise empty server, and on a configurable interval (default `60s`). A broadcast that never lands costs latency, never correctness — this is what makes the empty-server limitation survivable rather than fatal.

### Schema

Three tables, all prefixed by the configured table prefix.

```sql
CREATE TABLE IF NOT EXISTS `{prefix}reports` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `report_uuid`   BINARY(16)      NOT NULL,
  `target_uuid`   BINARY(16)      NOT NULL,
  `target_name`   VARCHAR(16)     NOT NULL,
  `reporter_uuid` BINARY(16)      NOT NULL,
  `reporter_name` VARCHAR(16)     NOT NULL,
  `reason`        VARCHAR(256)    NOT NULL,
  `origin_server` VARCHAR(64)     NOT NULL,
  `created_at`    BIGINT          NOT NULL,
  `expires_at`    BIGINT          NOT NULL,
  `dismissed_at`  BIGINT          NULL,
  `dismissed_by`  BINARY(16)      NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_uuid` (`report_uuid`),
  KEY `idx_valid` (`dismissed_at`, `expires_at`),
  KEY `idx_reporter_created` (`reporter_uuid`, `created_at`),
  KEY `idx_created` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `{prefix}report_counts` (
  `player_uuid`   BINARY(16)   NOT NULL,
  `player_name`   VARCHAR(16)  NOT NULL,
  `report_count`  INT UNSIGNED NOT NULL DEFAULT 0,
  `last_reported` BIGINT       NOT NULL,
  PRIMARY KEY (`player_uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `{prefix}staff_settings` (
  `player_uuid`           BINARY(16) NOT NULL,
  `notifications_enabled` TINYINT(1) NOT NULL,
  `updated_at`            BIGINT     NOT NULL,
  PRIMARY KEY (`player_uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

Three consequences worth naming:

- **`report_counts` is keyed by UUID and incremented separately from `reports`.** That is what makes the lifetime count survive both a player rename and the retention sweep that deletes old rows from `reports`.
- **Validity is derived, never stored as a flag.** A report is valid when `dismissed_at IS NULL AND expires_at > now`. There is no expiry job to fall behind, and every server computes the same answer from the same row.
- **The submission cooldown is derived from `MAX(created_at)` for the reporter**, not held in memory. It is therefore network-wide by construction and survives a restart, and a rejected submission cannot start it because nothing was inserted.

### Wire protocol

Channel `sreports:main`. Every frame is `[byte protocolVersion][byte frameType][payload]`, written with `DataOutputStream`; UUIDs as two longs, strings as UTF. A frame whose version is unknown, whose type is unrecognised, or whose payload is truncated is logged and dropped without touching state. Backends additionally reject any frame on this channel that did not arrive from the proxy.

Frames form a sealed interface in `s-reports-common`, so the codec's `switch` is exhaustive at compile time and adding a frame without handling it is a build error:

| Frame | Direction | Purpose |
| --- | --- | --- |
| `TargetResolveRequest` | Paper → Velocity | Ask who and where the target is |
| `TargetProbe` | Velocity → Paper(target) | Read the target's exempt permission locally |
| `TargetProbeResult` | Paper(target) → Velocity | Answer the probe |
| `TargetResolveResponse` | Velocity → Paper(origin) | Resolution result or a deny reason |
| `ReportCreated` | Paper → Velocity → all | A new valid report |
| `ReportDismissed` | Paper → Velocity → all | A report left the valid set |
| `TeleportRequest` | Paper → Velocity | Staff wants to reach a target |
| `TeleportArm` | Velocity → Paper(target) | Pre-register the pending teleport |
| `TeleportGrant` | Velocity → Paper(origin) | Same-server teleport, go ahead |
| `TeleportDenied` | Velocity → Paper(origin) | Offline, moved, or transfer failed |
| `SyncRequest` | Paper → Velocity | Backend woke up and wants to catch up |

`SyncRequest` triggers a database reconciliation rather than a large sync payload, which keeps every frame comfortably inside the payload ceiling.

### Threading

All database work runs on `Executors.newVirtualThreadPerTaskExecutor()`. Java 25 no longer pins a virtual thread inside `synchronized`, so blocking JDBC calls behind HikariCP are safe to run this way. Results return to the main thread via the Paper scheduler before any world, inventory, or player state is touched. Nothing in a command handler or click handler blocks.

### Commands

Paper commands register through the Brigadier `LifecycleEvents.COMMANDS` lifecycle, the current Paper idiom, rather than a legacy `plugin.yml` command block. `/reports` and `/reports <keyword>` are one command with an optional greedy-string argument. `/report <target> <reason>` uses a player-name suggestion argument plus a greedy string.

### Menu

Plain Bukkit `Inventory` with a custom `InventoryHolder` used as the identity marker, so click handling never guesses from the title. Clicks are cancelled unconditionally before any action runs. `ClickType` selects behaviour: left variants teleport, right variants dismiss. Entries render as configurable items — player heads by default — with MiniMessage-formatted lore drawn from `menu.yml`.

The menu snapshots its report list when opened. Dismissal mutates the snapshot and re-renders in place; it does not re-query. Reports that expire while the menu is open remain visible until reopened, and are rejected with the report-unavailable message if clicked — a deliberate simplification over live-refreshing every open inventory.

*Alternative considered:* a third-party GUI library. Rejected to keep the dependency set to exactly what the network already runs.

### Message formatting

All strings live in `messages.yml` in MiniMessage format, which is what makes the clickable teleport component in the notification an ordinary configuration value rather than code:

```yaml
notification:
  # <click:run_command> targets the hidden staff command that resolves the report id.
  format: "<gray>[<red>Report</red>]</gray> <hover:show_text:'<green>Click to teleport'><click:run_command:'/sreportstp %report_id%'><white>%target%</white></click></hover> <gray>reported by</gray> <white>%reporter%</white> <gray>on</gray> <white>%server%</white>"
```

### Configuration surface

Paper `plugins/s-reports/`:

- `config.yml` — database connection, cooldown, TTL, retention, reconciliation interval, resolve and teleport timeouts, reason bounds, duplicate suppression, return-position toggle, default notification state, permission nodes.
- `menu.yml` — title, rows, entry slots, entry material and format, pagination control slots and materials, empty-state entry.
- `messages.yml` — the full message catalogue.

Velocity `plugins/s-reports/config.yml` — database connection and protocol/logging settings only. No message catalogue: the proxy renders nothing to players.

Durations parse from `30s` / `1h` / `90d` strings into `Duration`. A malformed or out-of-range value logs the offending key and falls back to the documented default; an unparseable file disables report functionality and leaves the server running.

### Enforcing the no-comment rule

A Checkstyle ruleset fails the build on any line in `src/main/java` whose first non-whitespace characters are `//`, `/*`, `*`, or `*/`. Anchoring to line start is what keeps a JDBC URL such as `jdbc:mysql://host` from tripping the check. `.yml` files are outside Checkstyle's scope and keep their explanatory comments.

### Version control workflow

Implementation happens on a dedicated branch (`add-cross-server-report-system`) cut from `main`, never committed directly to `main`. Commits land at the end of each numbered task group in `tasks.md` — one commit per group — so history mirrors the plan's own structure. Nothing is pushed to a remote as part of this workflow; that remains a separate, explicit step.

## Risks / Trade-offs

- **An empty backend server receives no broadcasts.** → Database reconciliation on enable, on first join, and on a fixed interval. Correctness never depends on a frame landing; only latency does.
- **The proxy is a single point of failure for resolution and teleports.** → Report submission degrades to a clear "cannot resolve target" message rather than writing a report against an unverified name. Browsing and dismissal continue to work, since both read from MySQL.
- **Two independent MySQL pools (Paper and Velocity) double the connection footprint.** → Small default pool sizes, both configurable; the alternative was a payload-chunking sync protocol, which is worse.
- **Menu snapshots go stale while open.** → Every click revalidates against storage before acting, so a stale entry produces a message, never a wrong teleport or a double dismissal.
- **The no-comment Checkstyle rule is a line-anchored heuristic**, not a parser. A comment appended to the end of a code line would slip through. → Accepted; the rule catches the realistic cases and the constraint is primarily a review convention.
- **Player-name arguments are ambiguous across a network** if two servers somehow hold the same name. → Velocity resolves by exact name against its single roster, which cannot contain duplicates.
- **MiniMessage in configuration lets an operator author a `run_command` component pointing anywhere.** → The teleport command is permission-gated and validates the report id against storage, so a crafted component grants nothing the operator did not already have.
- **Retention deletes rows from `reports` that a very long-lived menu might still reference.** → Click revalidation returns the report-unavailable message.

## Migration Plan

Greenfield: there is nothing to migrate from. Deployment order matters, though.

1. Provision the MySQL database and a user with `CREATE`, `SELECT`, `INSERT`, `UPDATE`, `DELETE` on it.
2. Install the Velocity plugin, start the proxy once to write `config.yml`, fill in credentials, restart. Tables are created on first successful connection.
3. Install the Paper plugin on one backend server, start it once to write its three configuration files, fill in credentials and permission nodes, restart.
4. Verify end-to-end on that single server: file a report, open the menu, left-click, right-click.
5. Roll out to the remaining backend servers, then verify cross-server: report on server A, browse and teleport from server B.

**Rollback:** remove the plugin jars and restart. Nothing outside the plugin's own tables is touched, and those tables can be dropped independently. No world data, player data, or server configuration is modified.

## Open Questions

- **Minimum MySQL version to target.** The schema uses only long-standing InnoDB features and no window functions, so it should run on 5.7 and MariaDB 10.x as well as MySQL 8.x. Confirming the deployment target lets the driver version and the connection URL defaults be pinned precisely; it does not change the schema or any task.
- **Whether the hidden `/sreportstp <report-id>` command backing the clickable notification should be listed in tab-completion.** Hiding it is the default; either way is a one-line change with no spec impact.
