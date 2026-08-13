# s-reports

[![CI](https://github.com/ViniciusSambinello/s-report/actions/workflows/ci.yml/badge.svg)](https://github.com/ViniciusSambinello/s-report/actions/workflows/ci.yml)

A network-wide player reporting system for Paper servers behind a Velocity proxy, with MySQL persistence, a live-refreshing staff inventory menu, cross-server teleport, and live chat notifications.

## Why

A multi-server Velocity network has no shared view of player reports: a report filed on `survival-1` is invisible to a staff member standing on `survival-2`, and there's no way to tell a first-time complaint from a repeat offender. s-reports replaces that with a single source of truth in MySQL, so any player can report from any backend server and any staff member — regardless of which server they're on — sees it immediately and can jump straight to the reported player.

## Features

- **`/report <player> <reason>`** from any backend server, with a configurable per-player cooldown.
- **Network-wide `/reports [keyword]` menu** listing every currently valid report, showing the target's name, the reporter's name, the reason, the target's current valid report count, and the server the target is on.
- **Live-refreshing menu** — the time-remaining countdown updates in real time, and entries drop out on their own once expired, without reopening the menu.
- **Left-click to teleport**, transferring the staff member cross-server first when needed; **right-click to dismiss**, network-wide.
- **`/togglereport`** — per-staff opt-in/opt-out of live chat notifications, which carry a clickable teleport component.
- Reports expire on a configurable TTL and stay in permanent per-player history even after they expire or are dismissed.
- Every timing, permission node, menu layout, and player-facing message is a `.yml` value — no code changes needed to reconfigure.

## Requirements

- Java 25 on every Paper backend and on the Velocity proxy.
- Paper API `26.2`.
- Velocity `4.0.0`.
- MySQL 8.0+, reachable from every backend and from the proxy (the schema avoids window functions, so 5.7 and MariaDB 10.x should also work).
- Velocity modern player-info forwarding already configured between the proxy and its backends.

## Installation

1. Provision the MySQL database and an application user; confirm it's reachable from every backend and from the proxy. Tables are created automatically on first successful connection.
2. Drop `s-reports-velocity-<version>.jar` into the proxy's `plugins/` folder, start it once to generate `plugins/s-reports/config.yml`, fill in the database credentials, and restart.
3. Drop `s-reports-paper-<version>.jar` into one backend's `plugins/` folder, start it once to generate `plugins/s-reports/{config,menu,messages}.yml`, fill in the database credentials, set `server-name` to match the name Velocity's `velocity.toml` uses for that server, grant permission nodes, and restart.
4. Verify on that single server: file a report, open `/reports`, left-click, right-click.
5. Copy the same setup to every other backend, changing only `server-name` on each, then verify cross-server: report on server A, browse and teleport from server B.

Removing the plugin jars and restarting rolls everything back — no world data, player data, or server configuration is touched, and the plugin's own MySQL tables can be dropped independently.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/report <player> <reason>` | File a report. | `sreports.report` |
| `/reports [keyword]` | Open the report menu, optionally filtered. | `sreports.browse` |
| `/togglereport` | Toggle live report notifications. | `sreports.notify` |
| `/sreportstp <report-id>` | Teleport via a notification's clickable component (not tab-completed). | `sreports.notify` |

## Permissions

| Permission | Grants |
| --- | --- |
| `sreports.report` | File reports with `/report`. |
| `sreports.cooldown.bypass` | Skip the submission cooldown. |
| `sreports.exempt` | Can never be reported. |
| `sreports.browse` | Open `/reports`. |
| `sreports.dismiss` | Right-click dismiss a report entry. |
| `sreports.notify` | Receive live notifications, use `/togglereport`, and teleport from a notification click. |

All node names above are configurable in `config.yml` under `permissions:`.

## Configuration

s-reports ships sensible defaults for every config file, generated automatically on first boot. A minimal Paper `config.yml` database section looks like:

```yaml
server-name: survival-1

database:
  host: localhost
  port: 3306
  database: sreports
  user: sreports
  password: "changeme"
  table-prefix: "sreports_"
```

| File | Covers |
| --- | --- |
| `config.yml` | Database connection, cooldown, report TTL, retention, reconciliation and menu-refresh intervals, resolve/teleport timeouts, reason length bounds, duplicate suppression, return-position toggle, default notification state, permission nodes. |
| `menu.yml` | Menu title, rows, entry slots, entry item/format, pagination controls, empty-state entry. |
| `messages.yml` | Every player-facing message, in MiniMessage format. |

Velocity's `config.yml` only covers its own database connection and retention timing — the proxy renders nothing to players. Every duration setting accepts a human-readable form: `30s`, `45m`, `1h`, `90d`.

## Documentation

Velocity and each Paper server open independent MySQL connection pools and stay in sync two ways: report events broadcast immediately over a custom binary protocol on the `sreports:main` plugin messaging channel, and periodic reconciliation of valid reports directly from MySQL — so nothing is lost even if a backend was empty when a report was filed, or a message never lands.

- [design.md](openspec/changes/add-cross-server-report-system/design.md) — full design rationale, MySQL schema, and wire protocol.
- [proposal.md](openspec/changes/add-cross-server-report-system/proposal.md) — the original problem statement and capability breakdown.

## Building from source

```bash
./gradlew build
```

Runs the full test suite (including a Testcontainers-backed MySQL suite — a local Docker daemon is required) and a Checkstyle pass that fails on any comment inside `src/main/java` (comments are only permitted in `.yml` files). Produces `s-reports-paper/build/libs/s-reports-paper-<version>.jar` and `s-reports-velocity/build/libs/s-reports-velocity-<version>.jar`.

## License

[MIT](LICENSE).
