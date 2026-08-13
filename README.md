# s-reports

Network-wide player reporting for a [Velocity](https://papermc.io/software/velocity) proxy backed by [Paper](https://papermc.io/software/paper) servers. Any player can report another player from any backend server; any staff member, regardless of which server they're on, sees the report, its context, and can jump straight to the reported player with one click.

## Features

- **`/report <player> <reason>`** — file a report from any backend server, subject to a configurable per-player cooldown.
- **`/reports [keyword]`** — a paginated staff inventory menu listing every currently valid report on the network, optionally filtered by a keyword in the reason. Each entry shows the target's name, the reporter's name, the reason, the target's current valid report count, and the server the target is on (or an offline indicator).
- **Live-updating menu** — the time-remaining countdown on every open entry updates in real time, and an entry disappears on its own once the report expires, without needing to reopen the menu.
- **Left-click to teleport** — transfers the staff member to the target's backend server (via Velocity) and teleports them to the target on arrival.
- **Right-click to dismiss** — removes a report from the valid set network-wide.
- **`/togglereport`** — per-staff opt-in/opt-out of live chat notifications, which carry a clickable teleport component.
- **`/sreportstp <report-id>`** — the hidden command backing that clickable notification.
- Reports expire automatically after a configurable TTL and stay in permanent per-player history even after they expire or are dismissed.
- Every timing, permission node, menu layout, and player-facing message is defined in `.yml` files — no code changes needed to reconfigure.

## Requirements

- Java 25
- A Velocity proxy (built and tested against 4.0.0)
- One or more Paper backend servers (built and tested against Paper API `26.2`)
- A MySQL 8.x instance reachable from the proxy and every backend server (the schema avoids window functions, so MySQL 5.7 and MariaDB 10.x should also work)
- Velocity modern player-info forwarding already configured between the proxy and its backends (see [docs.papermc.io](https://docs.papermc.io/velocity/) for setup)

## Project layout

```
s-reports-common/    Shared domain model, MySQL persistence, wire protocol, config parsing — no platform dependency
s-reports-paper/     Paper plugin: commands, the report menu, teleport handling, notifications
s-reports-velocity/  Velocity plugin: target resolution, cross-server transfer, message routing
```

`s-reports-common` depends on neither platform API and holds nearly all of the unit-tested logic. Both platform modules produce a shaded jar with their own copies of HikariCP, the MySQL driver, and SnakeYAML relocated to avoid classpath collisions with other plugins.

## Installation

1. Provision a MySQL database and a user with `CREATE`, `SELECT`, `INSERT`, `UPDATE`, `DELETE` on it. Tables are created automatically on first successful connection.
2. Drop `s-reports-velocity-<version>.jar` into the proxy's `plugins/` folder, start it once to generate `plugins/s-reports/config.yml`, fill in the database credentials, and restart.
3. Drop `s-reports-paper-<version>.jar` into each backend server's `plugins/` folder, start it once to generate `plugins/s-reports/{config,menu,messages}.yml`, fill in the database credentials, set `server-name` to match the name Velocity's `velocity.toml` uses for that server, grant permission nodes, and restart.
4. Verify on a single server first: file a report, open `/reports`, left-click, right-click.
5. Roll out to the remaining backend servers, then verify cross-server: report on server A, browse and teleport from server B.

Removing the plugin jars and restarting rolls everything back — no world data, player data, or server configuration is touched, and the plugin's own MySQL tables can be dropped independently.

## Configuration

Paper (`plugins/s-reports/`):

| File | Covers |
|---|---|
| `config.yml` | Database connection, cooldown, report TTL, retention, reconciliation and menu-refresh intervals, resolve/teleport timeouts, reason length bounds, duplicate suppression, return-position toggle, default notification state, permission nodes |
| `menu.yml` | Menu title, rows, entry slots, entry item/format, pagination controls, empty-state entry |
| `messages.yml` | Every player-facing message, in [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format |

Velocity (`plugins/s-reports/config.yml`) only covers its own database connection and retention timing — the proxy renders nothing to players.

Every duration setting accepts a human-readable form: `30s`, `45m`, `1h`, `90d`.

## Commands & permissions

| Command | Permission | Description |
|---|---|---|
| `/report <player> <reason>` | `sreports.report` | File a report |
| `/reports [keyword]` | `sreports.browse` | Open the report menu, optionally filtered |
| `/togglereport` | `sreports.notify` | Toggle live report notifications |
| `/sreportstp <report-id>` | `sreports.notify` | Teleport via a notification's clickable component (not tab-completed) |

| Permission | Grants |
|---|---|
| `sreports.report` | File reports with `/report` |
| `sreports.cooldown.bypass` | Skip the submission cooldown |
| `sreports.exempt` | Can never be reported |
| `sreports.browse` | Open `/reports` |
| `sreports.dismiss` | Right-click dismiss a report entry |
| `sreports.notify` | Receive live notifications, use `/togglereport`, and teleport from a notification click |

All of the above node names are configurable in `config.yml` under `permissions:`.

## Building from source

```bash
./gradlew build
```

Produces `s-reports-paper/build/libs/s-reports-paper-<version>.jar` and `s-reports-velocity/build/libs/s-reports-velocity-<version>.jar`. The build runs the full test suite (including a Testcontainers-backed MySQL integration suite — a local Docker daemon is required) and a Checkstyle pass that fails on any comment inside `src/main/java` (comments are only permitted in `.yml` files).

## Architecture

Velocity and each Paper server open independent MySQL connection pools and stay in sync two ways: report events (created, dismissed, teleport handshakes) are broadcast immediately over a custom binary protocol on the `sreports:main` plugin messaging channel, and every server periodically reconciles its view of valid reports directly from MySQL — so a report is never lost even if a backend server was empty when it was filed, or a message never lands.

See `openspec/changes/add-cross-server-report-system/design.md` for the full design rationale, schema, and wire protocol.
