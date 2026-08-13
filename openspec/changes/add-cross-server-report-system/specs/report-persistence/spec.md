## Purpose

Defines the durable storage of reports in MySQL, the permanent per-player record of reports received, the time-based expiry of reports, and how the system behaves when the database is unreachable.

## ADDED Requirements

### Requirement: Durable report storage

The system SHALL persist every accepted report to MySQL. A stored report SHALL retain the target's unique player identifier and name, the reporter's unique player identifier and name, the reason text, the server the target was on at submission time, the submission timestamp, and the expiry timestamp. Stored reports SHALL survive a restart of any backend server and of the proxy.

#### Scenario: Report persisted on submission

- **WHEN** a report is accepted
- **THEN** the report is written to MySQL with target identity, reporter identity, reason, origin server, submission timestamp, and expiry timestamp

#### Scenario: Reports survive a restart

- **WHEN** a backend server holding valid reports restarts
- **THEN** the valid reports are still listed in the menu after the restart

#### Scenario: Schema created on first start

- **WHEN** the system starts against a database that does not yet contain its tables
- **THEN** the system creates the required tables and indexes
- **AND** the system starts successfully

#### Scenario: Names recorded for offline display

- **WHEN** a report's target and reporter are both offline
- **THEN** the menu still displays the names recorded at submission time

### Requirement: Report history per player

The system SHALL maintain a permanent count of how many reports each player has ever received, stored independently of the report menu's own display. This count SHALL include reports that have expired and reports that have been dismissed. The count SHALL be readable from any backend server. This stored lifetime count is distinct from the valid report count shown on a menu entry, which reflects only the target's currently valid reports — see `report-browsing`'s Report entry information requirement.

#### Scenario: Count increments on each report

- **WHEN** `Steve` has received 3 reports and a fourth report is filed against him
- **THEN** the recorded lifetime report count for `Steve` becomes 4

#### Scenario: Expired reports remain counted

- **WHEN** all reports against `Steve` have expired and a new report is filed against him
- **THEN** the lifetime report count for `Steve` includes the expired reports

#### Scenario: Dismissed reports remain counted

- **WHEN** a report against `Steve` is dismissed
- **THEN** the lifetime report count for `Steve` still includes the dismissed report

#### Scenario: Count is consistent across servers

- **WHEN** a report is filed against `Steve` on `survival-1`
- **THEN** the recorded lifetime report count for `Steve`, read from `survival-2`, includes the report filed on `survival-1`

#### Scenario: Player renamed

- **WHEN** a player who has received reports under a previous name is reported again under a new name
- **THEN** the lifetime report count includes the reports received under the previous name

### Requirement: Report expiry

Each report SHALL carry an expiry time computed from a configurable time-to-live, defaulting to 1 hour. Once a report has passed its expiry time the system SHALL stop treating it as valid, excluding it from the staff menu and from teleport actions. Expired reports SHALL be retained for history and SHALL NOT be deleted by expiry alone.

#### Scenario: Report expires after the configured TTL

- **WHEN** the configured time-to-live is 1 hour and a report was filed more than 1 hour ago
- **THEN** the report is no longer listed in the staff menu

#### Scenario: Custom TTL is applied

- **WHEN** the configured time-to-live is 30 minutes and a report is filed
- **THEN** the report's expiry time is 30 minutes after its submission time

#### Scenario: Expired report retained for history

- **WHEN** a report expires
- **THEN** the report record remains in storage
- **AND** it continues to count toward the target's lifetime report count

#### Scenario: Teleport to an expired report

- **WHEN** a staff member attempts to teleport using a report that has expired
- **THEN** the staff member is not teleported
- **AND** the staff member receives the configured report-unavailable message

### Requirement: Report dismissal record

When a report is dismissed the system SHALL record that it was dismissed, by whom, and when. A dismissed report SHALL no longer be valid but SHALL remain in storage.

#### Scenario: Dismissal is recorded

- **WHEN** a staff member dismisses a report
- **THEN** the stored report records the dismissing staff member's identity and the dismissal time

#### Scenario: Dismissed report is not valid

- **WHEN** a report has been dismissed
- **THEN** it is excluded from the staff menu on every backend server

### Requirement: Storage retention

The system SHALL delete stored reports older than a configurable retention period, defaulting to 90 days, so history does not grow without bound. Retention SHALL be enforced by a periodic cleanup. Setting the retention period to zero SHALL disable deletion entirely.

#### Scenario: Old reports are removed

- **WHEN** the retention period is 90 days and the cleanup runs against reports older than 90 days
- **THEN** those reports are deleted from storage

#### Scenario: Recent reports are kept

- **WHEN** the cleanup runs and a report is newer than the retention period
- **THEN** that report is not deleted

#### Scenario: Retention disabled

- **WHEN** the retention period is configured as zero and the cleanup runs
- **THEN** no reports are deleted

### Requirement: Database availability handling

When the database is unreachable, the system SHALL reject new report submissions with a clear message rather than silently discarding them, SHALL log the failure, and SHALL keep the rest of the server functioning. The system SHALL retry establishing the connection and SHALL resume normal operation once it succeeds.

#### Scenario: Submission while the database is down

- **WHEN** a player files a report while the database is unreachable
- **THEN** the report is not recorded
- **AND** the reporter receives the configured storage-unavailable message
- **AND** the failure is logged

#### Scenario: Menu while the database is down

- **WHEN** a staff member opens the report menu while the database is unreachable
- **THEN** the system does not crash the server
- **AND** the staff member receives the configured storage-unavailable message

#### Scenario: Recovery after an outage

- **WHEN** the database becomes reachable again after an outage
- **THEN** report submission and browsing resume working without a server restart

#### Scenario: Invalid credentials at startup

- **WHEN** the system starts with database credentials it cannot authenticate with
- **THEN** the failure is logged with a clear message
- **AND** report commands report the storage as unavailable rather than failing silently

### Requirement: Non-blocking database access

Database operations SHALL NOT block the server's main game thread. Command handling and menu interaction SHALL remain responsive while a database operation is in flight.

#### Scenario: Slow query does not freeze the server

- **WHEN** a database query takes several seconds to complete
- **THEN** the server continues ticking normally
- **AND** the requesting player receives the result once the query completes
