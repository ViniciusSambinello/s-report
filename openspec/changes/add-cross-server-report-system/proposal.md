## Why

A multi-server Minecraft network running Velocity has no shared view of player reports: a report filed on `survival-1` is invisible to a staff member standing on `survival-2`, so rule-breakers are handled late or not at all. Staff also have no history of how often a given player has been reported, which makes repeat offenders indistinguishable from one-off complaints.

`s-reports` introduces a network-wide report system where any player can report another player from any backend server, and any staff member — regardless of which server they are on — can see the report, read its context, and be moved directly to the reported player in a single click.

## What Changes

- New Gradle (Kotlin DSL) multi-module project `s-reports` targeting **Java 25**, producing two shaded artifacts: a **Paper 26.2** plugin and a **Velocity** plugin sharing a common module.
- Players gain `/report <target> <reason>`, subject to a configurable per-player cooldown (default `30s`).
- Reports are broadcast network-wide over a Velocity plugin messaging channel and persisted to **MySQL**, so every backend server observes the same report set.
- Staff gain `/reports`, a paginated inventory menu listing every valid (unexpired, unresolved) report with target name, reporter name, reason, the target's lifetime report count, and the server the target is currently on.
- Staff gain `/reports <keyword>`, opening the same menu filtered to reports whose reason contains the keyword.
- Menu interactions: **left-click** transfers the staff member to the target's server (via Velocity) and teleports them to the target on arrival; **right-click** dismisses the report, removing it from the valid set network-wide.
- Staff gain `/togglereport`, enabling or disabling live chat notifications for incoming reports. Notifications carry a clickable component that performs the same cross-server transfer-and-teleport.
- Reports expire automatically after a configurable TTL (default `1h`); expired and dismissed reports leave the valid set but remain in the target's permanent report history.
- All behaviour, thresholds, database credentials, menu layout, and every user-facing message are defined in `.yml` configuration files on both platforms.
- Codebase conventions: all identifiers, log output, and player-facing text in **English**; **no comments in Java source** — explanatory comments appear only in `.yml` configuration files; immutable-first style with `final` fields, `final` locals, records for value types, and unmodifiable collections.

## Capabilities

### New Capabilities

- `report-submission`: Filing a report against another player, target and reason validation, per-player submission cooldown, and self/exempt-target rules.
- `report-browsing`: The staff inventory menu listing valid reports, keyword filtering by reason, pagination, and the left-click/right-click interaction contract.
- `report-notifications`: Per-staff opt-in/opt-out of live report notifications and the clickable chat component delivered network-wide.
- `report-routing`: Cross-server delivery of report events over Velocity plugin messaging, plus the transfer-then-teleport flow that moves a staff member to a target on a different backend server.
- `report-persistence`: MySQL storage of reports and per-player report history, TTL-based expiry, dismissal, and the lifetime report count surfaced in the menu.
- `report-configuration`: The `.yml` configuration surface for both platforms — database settings, timings, permissions, menu layout, and message catalogue.

### Modified Capabilities

None — this is a greenfield project with no existing specs.

## Impact

- **New repository content**: `settings.gradle.kts`, root `build.gradle.kts`, and modules `s-reports-common`, `s-reports-paper`, `s-reports-velocity`.
- **Runtime dependencies**: Paper API 26.2, Velocity API, MySQL Connector/J, HikariCP, SnakeYAML (Paper-side; Velocity ships its own YAML/config handling), Adventure API (bundled by both platforms).
- **Infrastructure**: a reachable MySQL instance shared by all backend servers; two new tables plus their indexes.
- **Network configuration**: a custom plugin messaging channel registered on every Paper server and on Velocity; requires `player-info-forwarding` already configured for the proxy.
- **Operational constraint**: because plugin messaging is carried on player connections, a backend server with zero online players cannot originate or receive messages. Report state is reconciled from MySQL on server join and on a configurable interval so no report is permanently lost.
- **Permissions**: new permission nodes for filing reports, viewing the menu, receiving notifications, dismissing reports, and being exempt from being reported.
