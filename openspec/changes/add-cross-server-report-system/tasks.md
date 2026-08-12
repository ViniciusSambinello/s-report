## 0. Version Control

- [x] 0.1 Create and switch to a git branch named `add-cross-server-report-system` from `main` before making any file changes.
- [ ] 0.2 Commit at the end of each numbered group below (1 through 14) on that branch; do not push to a remote unless separately requested.

## 1. Project Scaffolding

- [x] 1.1 Create `settings.gradle.kts` declaring root project `s-reports` and modules `s-reports-common`, `s-reports-paper`, `s-reports-velocity`, with the PaperMC and Maven Central repositories.
- [x] 1.2 Create the root `build.gradle.kts` applying the Java toolchain at language version 25 to all subprojects, setting UTF-8 encoding, and enabling `-parameters`.
- [x] 1.3 Add the `com.gradleup.shadow` plugin to both platform modules, configured to relocate `com.zaxxer.hikari`, `com.mysql`, and `org.yaml.snakeyaml` under an `s-reports` shade package.
- [x] 1.4 Add Checkstyle with a ruleset that fails the build on any line in `src/main/java` whose first non-whitespace characters are `//`, `/*`, `*`, or `*/`, and wire it into `check`.
- [x] 1.5 Add the Gradle wrapper and a `.gitignore` covering `build/`, `.gradle/`, and IDE directories.
- [x] 1.6 Verify `./gradlew build` succeeds on the empty module skeletons.

## 2. Common — Domain Model

- [x] 2.1 Create the `Report` record holding report id, target id and name, reporter id and name, reason, origin server, created-at, expires-at, dismissed-at, and dismissed-by, with a validity predicate derived from dismissal and expiry against a supplied instant.
- [x] 2.2 Create the `ReportView` record combining a `Report` with the target's lifetime report count and currently resolved server, as consumed by the menu and notifications.
- [x] 2.3 Create the `TargetResolution` record carrying target id, name, current server, and exempt flag, plus a `ResolveFailure` enum for not-found, self, and exempt outcomes.
- [x] 2.4 Create the `TeleportDenyReason` enum covering target-offline, target-moved, transfer-failed, and report-unavailable.
- [x] 2.5 Create the `StaffSettings` record holding player id and notification-enabled flag.
- [x] 2.6 Implement `ReportFilter` performing case-insensitive substring matching of a keyword against a report reason, returning an unmodifiable filtered list.
- [x] 2.7 Confirm every type in this group is a record or enum, exposes only `final` state, and returns collections through `List.copyOf`.

## 3. Common — Configuration

- [x] 3.1 Implement `DurationParser` converting `30s`, `45m`, `1h`, `90d` style strings to `Duration`, rejecting malformed input with a typed failure rather than an exception at call sites.
- [x] 3.2 Implement the YAML loading layer that reads a file, writes the packaged default when absent, and never overwrites an existing operator file.
- [x] 3.3 Implement per-key validated accessors that log the offending key and fall back to a documented default when a value is missing, malformed, or out of range.
- [x] 3.4 Implement the unparseable-file path: log the parse error with the file name, mark configuration as failed, and expose that state so the plugin can disable report functionality while leaving the server running.
- [x] 3.5 Create the `DatabaseConfig` record for host, port, database, user, password, pool size, connection timeout, and table prefix.
- [x] 3.6 Create the `BehaviourConfig` record for cooldown, report TTL, retention period, reconciliation interval, resolve timeout, pending-teleport timeout, reason minimum and maximum length, duplicate suppression, return-position toggle, and default notification state.
- [x] 3.7 Create the `PermissionConfig` record for the report, cooldown-bypass, exempt, browse, dismiss, and notify permission nodes.
- [x] 3.8 Add the "missing key added by an upgrade" path: apply the documented default and log which setting fell back.

## 4. Common — Persistence

- [x] 4.1 Add the MySQL Connector/J and HikariCP dependencies and implement the pooled `DataSource` factory from `DatabaseConfig`.
- [x] 4.2 Implement schema creation for `{prefix}reports`, `{prefix}report_counts`, and `{prefix}staff_settings` with the indexes defined in `design.md`, executed idempotently on first connection.
- [x] 4.3 Implement `ReportRepository.insert` writing the report row and incrementing `report_counts` via `INSERT ... ON DUPLICATE KEY UPDATE` in a single transaction, refreshing the stored player name.
- [x] 4.4 Implement `ReportRepository.findValid` returning reports where `dismissed_at IS NULL AND expires_at > now`, joined to their targets' lifetime counts.
- [x] 4.5 Implement `ReportRepository.dismiss` setting `dismissed_at` and `dismissed_by` only when the row is still valid, returning whether it actually changed a row so a second dismissal can be reported as unavailable.
- [x] 4.6 Implement `ReportRepository.lastReportInstant` returning the reporter's most recent `created_at`, backing the network-wide submission cooldown.
- [x] 4.7 Implement `ReportRepository.hasValidReportFrom` backing duplicate suppression for a reporter and target pair.
- [x] 4.8 Implement `ReportRepository.deleteOlderThan` for retention, and make a retention period of zero a no-op.
- [x] 4.9 Implement `StaffSettingsRepository` load and upsert for the notification toggle.
- [x] 4.10 Implement the virtual-thread executor wrapper so every repository call returns a `CompletableFuture` and no call blocks a caller thread.
- [x] 4.11 Implement connection-failure handling: log once per outage, surface an unavailable state to callers, and recover without a restart when the database returns.
- [ ] 4.12 Verify UUIDs round-trip correctly through `BINARY(16)` and that the lifetime count survives a simulated player rename. (deferred to the Testcontainers suite in group 13)

## 5. Common — Wire Protocol

- [x] 5.1 Define the sealed `ReportFrame` interface and its record implementations for the eleven frames listed in `design.md`.
- [x] 5.2 Implement the codec writing `[protocolVersion][frameType][payload]` with `DataOutputStream`, encoding UUIDs as two longs and strings as UTF.
- [x] 5.3 Implement decoding with an exhaustive `switch` over the sealed hierarchy so an unhandled frame type is a compile error.
- [x] 5.4 Implement fail-closed decoding: unknown protocol version, unrecognised frame type, and truncated payload each log and return no frame without mutating state.
- [x] 5.5 Define the channel identifier `sreports:main` as a shared constant used by both platforms.
- [ ] 5.6 Add round-trip codec tests covering every frame type plus each malformed-input path. (deferred to group 13)

## 6. Velocity Plugin

- [x] 6.1 Create the `@Plugin`-annotated main class, register the `sreports:main` channel identifier, and load `config.yml`, writing defaults on first start.
- [x] 6.2 Open the Velocity-side MySQL pool from `DatabaseConfig` and run schema creation.
- [x] 6.3 Implement the plugin message listener, rejecting frames that did not originate from a backend server connection and cancelling the event so frames never reach clients.
- [x] 6.4 Implement target resolution: look up the named player on the proxy roster, and on failure return `TargetResolveResponse` carrying `NOT_FOUND` to the origin server.
- [x] 6.5 Implement the probe hop: forward `TargetProbe` to the target's backend server and route the returned `TargetProbeResult` back to the origin server as a `TargetResolveResponse`.
- [x] 6.6 Implement broadcast of `ReportCreated` and `ReportDismissed` to every registered server that has at least one online player, logging servers skipped as unreachable.
- [x] 6.7 Implement the teleport flow: resolve the target's current server, send `TeleportArm` to that server, then issue the connection request, mapping same-server to `TeleportGrant` and failures to `TeleportDenied`.
- [x] 6.8 Implement `SyncRequest` handling by instructing the requesting server to reconcile.
- [x] 6.9 Implement the retention cleanup task on the configured interval.
- [x] 6.10 Verify the proxy plugin contains no report business logic and renders no player-facing text.

## 7. Paper Plugin — Bootstrap

- [x] 7.1 Create `paper-plugin.yml` targeting the Paper 26.2 API and the plugin main class.
- [x] 7.2 Load `config.yml`, `menu.yml`, and `messages.yml` on enable, writing packaged defaults when absent. (packaged default resources themselves are authored in group 12)
- [x] 7.3 Open the Paper-side MySQL pool, run schema creation, and disable report functionality with a clear log message if configuration failed to parse.
- [x] 7.4 Register the `sreports:main` incoming and outgoing channels and the frame dispatcher, ignoring frames that did not arrive from the proxy. (see note below)
- [x] 7.5 Implement the MiniMessage-backed message service resolving keys from `messages.yml`, substituting placeholders, and sending nothing when a message is configured as empty.
- [x] 7.6 Wire the reconciliation trigger on enable, on the first player joining an otherwise empty server, and on the configured interval.
- [x] 7.7 Implement the in-memory valid-report cache updated by broadcasts and replaced wholesale by reconciliation.

## 8. Paper Plugin — Report Submission

- [x] 8.1 Register `/report <target> <reason>` through the Brigadier `COMMANDS` lifecycle with a player-name suggestion argument and a greedy-string reason.
- [x] 8.2 Implement permission gating and the usage path for a missing or whitespace-only reason.
- [x] 8.3 Implement reason length validation against the configured minimum and maximum, with the length included in the rejection message.
- [x] 8.4 Implement the cooldown check from `lastReportInstant`, including remaining-time substitution, the cooldown-bypass permission, and the guarantee that a rejected submission never starts the cooldown.
- [x] 8.5 Send `TargetResolveRequest` and track the pending request by request id with the configured resolve timeout and its timeout message.
- [x] 8.6 Handle `TargetResolveResponse`: reject not-found, self, and exempt targets with their configured messages.
- [x] 8.7 Implement duplicate suppression when enabled, allowing a repeat once the earlier report was dismissed or expired.
- [x] 8.8 Persist the accepted report, compute expiry from the configured TTL, then send `ReportCreated` to the proxy and confirm to the reporter.
- [x] 8.9 Implement the storage-unavailable path so a submission is rejected with a clear message and logged rather than silently dropped.

## 9. Paper Plugin — Notifications

- [x] 9.1 On `ReportCreated`, deliver the notification to local players who hold the notify permission and have notifications enabled, excluding the reporter.
- [x] 9.2 Render the notification from `messages.yml` with target, reporter, reason, and origin server placeholders substituted.
- [x] 9.3 Add the clickable teleport component with its hover description, backed by a permission-gated hidden command that resolves the report id.
- [x] 9.4 Register `/togglereport`, gate it on the notify permission, flip and persist the setting, and confirm the resulting state.
- [x] 9.5 Load staff settings on join into a local cache, applying the configured default for a player who has never toggled.
- [ ] 9.6 Verify the setting persists across a reconnect and applies on every backend server. (mechanism implemented; live verification deferred to 14.5/14.6)

## 10. Paper Plugin — Report Menu

- [x] 10.1 Implement the custom `InventoryHolder` used as the menu identity marker.
- [x] 10.2 Register `/reports` and `/reports <keyword>` as one Brigadier command with an optional greedy-string argument, gated on the browse permission.
- [x] 10.3 Build the menu from `menu.yml`: title, rows, entry slots, entry material, pagination control slots and materials, and the empty-state entry.
- [x] 10.4 Render each entry with target name, reporter name, reason, lifetime report count, resolved current server, and time remaining, substituting the offline indicator when the target is not online.
- [x] 10.5 Implement pagination with next and previous controls that are only actionable when a page exists in that direction, preserving the active keyword filter.
- [x] 10.6 Apply keyword filtering through `ReportFilter` and show the empty-state entry when nothing matches.
- [x] 10.7 Cancel every click on the menu unconditionally before dispatching on `ClickType`.
- [x] 10.8 Implement left-click: revalidate the report, close the menu, and start the teleport flow.
- [x] 10.9 Implement right-click: gate on the dismiss permission, dismiss through the repository, report the already-dismissed case as unavailable, refresh the open menu in place, and broadcast `ReportDismissed`.
- [x] 10.10 Implement the invalid-slot fallback: log the offending slot and use the default layout rather than failing to enable.
- [x] 10.11 Implement the storage-unavailable path for opening the menu without crashing the server.

## 11. Paper Plugin — Teleport

- [x] 11.1 Send `TeleportRequest` on a teleport action from either the menu or a notification click.
- [x] 11.2 Handle `TeleportGrant` by teleporting to the target locally and sending the confirmation message.
- [x] 11.3 Handle `TeleportArm` by recording the pending teleport keyed by staff id with its expiry, before the transfer completes.
- [x] 11.4 On `PlayerJoinEvent`, complete a pending teleport only when it is unexpired and the target is still online on this server; otherwise send the target-moved or target-offline message. (see note below on target-moved)
- [x] 11.5 Expire pending teleports on the configured timeout so a later join does not trigger an unexpected teleport.
- [x] 11.6 Handle `TeleportDenied` by rendering the matching message for offline, moved, transfer-failed, and report-unavailable.
- [x] 11.7 Record the staff member's prior server and location before teleporting when the return-position option is enabled, and record nothing when it is disabled.
- [ ] 11.8 Verify a report remains valid and listed for other staff after a teleport. (true by construction — teleport never touches report state; live verification deferred to 14.5/14.6)

## 12. Configuration Defaults

- [x] 12.1 Author the packaged Paper `config.yml` with every setting, its documented default, and an explanatory comment, using `30s` cooldown, `1h` TTL, and `90d` retention.
- [x] 12.2 Author the packaged Paper `menu.yml` with the default layout, entry format placeholders, and pagination controls, each commented.
- [x] 12.3 Author the packaged Paper `messages.yml` covering every message referenced by tasks 8 through 11, in English, in MiniMessage format, each commented.
- [x] 12.4 Author the packaged Velocity `config.yml` with database and logging settings, each commented.
- [x] 12.5 Cross-check that every message key referenced in code exists in `messages.yml` and that no user-facing string is hard-coded. (grep-verified: 21/21 keys present, single MessageService call site sends any message)

## 13. Tests

- [ ] 13.1 Unit-test `DurationParser` across valid forms, malformed input, and boundary values.
- [ ] 13.2 Unit-test configuration validation: missing key, malformed value, out-of-range value, and unparseable file.
- [ ] 13.3 Unit-test `ReportFilter` for case-insensitive, substring, multi-word, and no-match cases.
- [ ] 13.4 Unit-test `Report` validity across unexpired, expired, and dismissed states.
- [ ] 13.5 Round-trip and fail-closed tests for the codec, covering all frames and every malformed path.
- [ ] 13.6 Repository tests against a Testcontainers MySQL instance covering insert, valid lookup, dismissal idempotence, cooldown lookup, duplicate detection, retention, and count-survives-rename.
- [ ] 13.7 Unit-test the pending-request and pending-teleport maps for timeout expiry and correct keying.
- [ ] 13.8 Verify `./gradlew build` runs the full suite and the Checkstyle no-comment rule passes on all source.

## 14. Verification and Delivery

- [ ] 14.1 Confirm no `.java` file under `src/main/java` contains a comment and that all comments live in `.yml` files.
- [ ] 14.2 Confirm every field and local is `final`, every data type is a record, and no returned collection is mutable.
- [ ] 14.3 Confirm every identifier, log line, and player-facing default string is in English.
- [ ] 14.4 Build both shaded jars and confirm relocation applied and no shaded class leaks into the default package space.
- [ ] 14.5 Manually verify the single-server path: report, notification, menu, left-click teleport, right-click dismiss, `/togglereport`.
- [ ] 14.6 Manually verify the cross-server path: report on server A, browse and left-click from server B, arriving next to the target on server A.
- [ ] 14.7 Manually verify the empty-server path: file a report while a backend is empty, join it, and confirm reconciliation surfaces the report.
- [ ] 14.8 Manually verify the degraded paths: proxy unavailable, database unavailable, and target disconnecting mid-transfer.
