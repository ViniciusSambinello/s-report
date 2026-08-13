## Purpose

Defines the YAML configuration surface exposed on both the Paper and Velocity sides, covering database settings, timings, permission nodes, menu layout, and the catalogue of user-facing messages, along with how invalid configuration is handled.

## ADDED Requirements

### Requirement: Configuration files are YAML

All operator-tunable settings SHALL be expressed in `.yml` files. The system SHALL write default configuration files on first start when they are absent. Configuration files SHALL carry explanatory comments describing each setting and its accepted values.

#### Scenario: Defaults written on first start

- **WHEN** the system starts and no configuration files exist in its data directory
- **THEN** the system writes complete default configuration files
- **AND** the system starts successfully using those defaults

#### Scenario: Existing configuration is preserved

- **WHEN** the system starts and configuration files already exist
- **THEN** the system does not overwrite the operator's values

#### Scenario: New setting added by an upgrade

- **WHEN** the system starts with a configuration file that predates a newly introduced setting
- **THEN** the system applies the documented default for the missing setting
- **AND** the system logs which setting fell back to its default

### Requirement: Database configuration

The configuration SHALL expose the MySQL host, port, database name, user, password, connection pool size, and connection timeout, along with a table name prefix. These SHALL be configurable independently on each platform that connects to the database.

#### Scenario: Operator points the system at a database

- **WHEN** an operator sets host, port, database, user, and password in the configuration and starts the system
- **THEN** the system connects to that database

#### Scenario: Table prefix applied

- **WHEN** an operator sets a table name prefix
- **THEN** the system creates and queries its tables using that prefix

### Requirement: Behaviour configuration

The configuration SHALL expose the report submission cooldown defaulting to 30 seconds, the report time-to-live defaulting to 1 hour, the storage retention period defaulting to 90 days, the reconciliation interval, the report menu's refresh interval defaulting to 1 second, the pending cross-server teleport timeout, the minimum and maximum reason length, whether duplicate report suppression is enabled, whether the staff return position is recorded, and the default notification state for staff who have never used the toggle. Duration settings SHALL accept a human-readable form such as `30s`, `1h`, or `90d`.

#### Scenario: Default cooldown

- **WHEN** the system starts with default configuration
- **THEN** the report submission cooldown is 30 seconds

#### Scenario: Default time-to-live

- **WHEN** the system starts with default configuration
- **THEN** a filed report expires 1 hour after submission

#### Scenario: Default menu refresh interval

- **WHEN** the system starts with default configuration
- **THEN** the report menu refreshes every 1 second while open

#### Scenario: Operator changes the time-to-live

- **WHEN** an operator sets the report time-to-live to `2h` and a report is filed
- **THEN** the report expires 2 hours after submission

#### Scenario: Human-readable durations accepted

- **WHEN** an operator sets the cooldown to `45s` and the time-to-live to `30m`
- **THEN** the system applies a 45 second cooldown and a 30 minute time-to-live

### Requirement: Permission configuration

The configuration SHALL expose the permission node used for each gated action: filing a report, bypassing the submission cooldown, being exempt from being reported, browsing the report menu, dismissing a report, and receiving report notifications. Operators SHALL be able to change any node without changing code.

#### Scenario: Operator renames a permission node

- **WHEN** an operator changes the report browsing permission node in the configuration and restarts
- **THEN** only players holding the new node can open the report menu

#### Scenario: Defaults documented

- **WHEN** an operator opens the default configuration
- **THEN** every permission node is present with its default value and an explanatory comment

### Requirement: Menu configuration

The configuration SHALL expose the report menu's title, its number of rows, the slots used for report entries, the slots and item types of the pagination controls, the item type and display format of a report entry, and the empty-state entry. The entry display format SHALL support placeholders for the target name, reporter name, reason, lifetime report count, target server, and time remaining before expiry.

#### Scenario: Operator changes the menu title

- **WHEN** an operator sets a custom menu title and a staff member opens the menu
- **THEN** the menu displays the custom title

#### Scenario: Operator changes the entry format

- **WHEN** an operator changes the report entry format and a staff member opens the menu
- **THEN** each entry renders using the custom format with placeholders substituted

#### Scenario: Placeholder substitution

- **WHEN** the entry format contains the target name, reporter name, reason, report count, and server placeholders
- **THEN** each placeholder is replaced with the corresponding value for that report

#### Scenario: Invalid slot configuration

- **WHEN** an operator configures a menu slot outside the range implied by the configured number of rows
- **THEN** the system logs the invalid slot
- **AND** the system falls back to the default layout rather than failing to start

### Requirement: Message configuration

Every user-facing message SHALL be defined in configuration rather than hard-coded, in English by default. Messages SHALL support colour and formatting, and SHALL support the placeholders relevant to their context. An operator SHALL be able to blank a message to suppress it.

#### Scenario: Operator customises a message

- **WHEN** an operator changes the submission confirmation message and a player files a report
- **THEN** the player receives the customised message

#### Scenario: Placeholders substituted in messages

- **WHEN** the cooldown message contains a remaining-time placeholder and a player is rate limited
- **THEN** the player receives the message with the actual remaining time substituted

#### Scenario: Message suppressed

- **WHEN** an operator sets a message to an empty value and the corresponding event occurs
- **THEN** no message is sent for that event
- **AND** the underlying behaviour is unchanged

### Requirement: Configuration validation

The system SHALL validate configuration values on load. When a value is missing, malformed, or outside its accepted range, the system SHALL log the offending key with an explanation and SHALL fall back to that setting's documented default rather than failing to start. When the configuration file itself cannot be parsed, the system SHALL log the parse error and SHALL disable report functionality rather than operating on undefined values.

#### Scenario: Malformed duration value

- **WHEN** an operator sets the report time-to-live to a value that is not a valid duration
- **THEN** the system logs the offending key and the reason
- **AND** the system uses the default time-to-live of 1 hour

#### Scenario: Out-of-range numeric value

- **WHEN** an operator sets a negative connection pool size
- **THEN** the system logs the offending key
- **AND** the system uses the default pool size

#### Scenario: Unparseable configuration file

- **WHEN** the configuration file contains invalid YAML
- **THEN** the system logs the parse error with the file name
- **AND** report functionality is disabled
- **AND** the server continues to run
