## Purpose

Defines how report events travel between backend servers through the Velocity proxy so every server shares one view of the report set, and how a staff member is moved across servers and placed next to a reported player.

## ADDED Requirements

### Requirement: Network-wide report propagation

When a report is filed on any backend server, the system SHALL propagate it to the proxy and from there to every other connected backend server, so that all servers converge on the same set of valid reports. Propagation SHALL also carry report dismissals and report expirations.

#### Scenario: New report reaches other servers

- **WHEN** a report is filed on `survival-1`
- **THEN** `survival-2` and every other connected backend server observe the report as valid

#### Scenario: Dismissal reaches other servers

- **WHEN** a report is dismissed on `survival-1`
- **THEN** `survival-2` and every other connected backend server stop treating the report as valid

#### Scenario: Expiry reaches other servers

- **WHEN** a report reaches its expiry time
- **THEN** every connected backend server stops treating the report as valid

#### Scenario: Server joining the network catches up

- **WHEN** a backend server starts and connects to the proxy while valid reports already exist
- **THEN** that server observes the existing valid reports

### Requirement: Target location resolution

The proxy SHALL be the authority on which backend server each online player is connected to. Backend servers SHALL resolve a report target's current server through the proxy rather than from the server recorded at submission time.

#### Scenario: Target moved after the report

- **WHEN** a report is filed while the target is on `survival-1` and the target then connects to `survival-2`
- **THEN** the system resolves the target's current server as `survival-2`

#### Scenario: Target left the network

- **WHEN** a report's target disconnects from the network
- **THEN** the system resolves the target as having no current server

### Requirement: Cross-server transfer and teleport

When a staff member requests a teleport to a report target on a different backend server, the system SHALL transfer the staff member to that server through the proxy and then teleport them to the target's location once they have joined. The teleport SHALL only occur if the target is still on that server when the staff member arrives.

#### Scenario: Transfer then teleport

- **WHEN** a staff member on `survival-1` requests a teleport to a target on `survival-2`
- **THEN** the proxy transfers the staff member to `survival-2`
- **AND** once the staff member has joined `survival-2` the system teleports them to the target's location

#### Scenario: Same server requires no transfer

- **WHEN** a staff member requests a teleport to a target on the staff member's own server
- **THEN** no transfer occurs
- **AND** the staff member is teleported directly to the target's location

#### Scenario: Target leaves during the transfer

- **WHEN** a staff member is transferred to `survival-2` for a target who disconnects before the staff member joins
- **THEN** the staff member is not teleported
- **AND** the staff member receives the configured target-offline message

#### Scenario: Target switches server during the transfer

- **WHEN** a staff member is transferred to `survival-2` for a target who moves to `survival-3` before the staff member joins
- **THEN** the staff member is not teleported on `survival-2`
- **AND** the staff member receives the configured target-moved message

#### Scenario: Pending teleport expires

- **WHEN** a staff member requests a cross-server teleport and does not join the destination server within the configured pending-teleport timeout
- **THEN** the pending teleport is discarded
- **AND** no teleport occurs when the staff member later joins that server

#### Scenario: Destination server is unavailable

- **WHEN** a staff member requests a teleport to a target on a backend server the proxy cannot connect them to
- **THEN** the staff member remains on their current server
- **AND** the staff member receives the configured transfer-failed message

### Requirement: Return position after teleport

When the configured return-position option is enabled, the system SHALL record the staff member's position before a report teleport so it can be restored, and SHALL restore it when the staff member requests a return.

#### Scenario: Position recorded before teleport

- **WHEN** the return-position option is enabled and a staff member teleports to a report target
- **THEN** the system records the staff member's server and location prior to the teleport

#### Scenario: Option disabled

- **WHEN** the return-position option is disabled and a staff member teleports to a report target
- **THEN** the system records no prior position

### Requirement: Delivery resilience

Because proxy plugin messaging is carried over player connections, a backend server with no online players cannot send or receive messages. The system SHALL treat the database as the source of truth and SHALL reconcile each backend server's view of valid reports from the database on server start, on the first player joining an otherwise empty server, and on a configurable interval.

#### Scenario: Report filed while a server is empty

- **WHEN** a report is filed while `survival-2` has no online players, and a player later joins `survival-2`
- **THEN** `survival-2` reconciles from the database
- **AND** a staff member opening the menu on `survival-2` sees the report

#### Scenario: Periodic reconciliation

- **WHEN** the configured reconciliation interval elapses on a backend server
- **THEN** that server refreshes its view of valid reports from the database

#### Scenario: Messaging unavailable

- **WHEN** a backend server cannot deliver a report message to the proxy
- **THEN** the report is still persisted to the database
- **AND** the failure is logged
- **AND** the report becomes visible on other servers at their next reconciliation

### Requirement: Message authenticity

The system SHALL ignore report messages received on its channel that are malformed, carry an unrecognised protocol version, or originate from a source other than the proxy. Ignored messages SHALL be logged and SHALL NOT alter report state.

#### Scenario: Malformed message

- **WHEN** a backend server receives a malformed message on the report channel
- **THEN** the message is ignored and logged
- **AND** no report state changes

#### Scenario: Unrecognised protocol version

- **WHEN** a backend server receives a report message carrying a protocol version it does not support
- **THEN** the message is ignored and logged
- **AND** no report state changes

#### Scenario: Message not originating from the proxy

- **WHEN** a backend server receives a report-channel message that did not come from the proxy
- **THEN** the message is ignored and logged
- **AND** no report state changes
