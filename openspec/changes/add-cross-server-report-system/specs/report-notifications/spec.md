## Purpose

Defines the live chat alert staff receive when a report is filed anywhere on the network, the clickable teleport component it carries, and the per-staff toggle that turns these alerts on and off.

## ADDED Requirements

### Requirement: Live report notification

When a report is successfully filed, the system SHALL deliver a chat notification to every online staff member on the network who holds the notification permission and has notifications enabled. The notification SHALL be delivered regardless of which backend server the staff member is on. The notification SHALL display the target's name, the reporter's name, the reason, and the server the target was on when the report was filed.

#### Scenario: Staff on another server is notified

- **WHEN** a player on `survival-1` files a report and a staff member with notifications enabled is online on `survival-2`
- **THEN** that staff member receives the report notification in chat

#### Scenario: Notification content

- **WHEN** `Alex` on `survival-1` reports `Steve` for `using kill aura`
- **THEN** the notification displays the target name `Steve`
- **AND** the notification displays the reporter name `Alex`
- **AND** the notification displays the reason `using kill aura`
- **AND** the notification displays the server `survival-1`

#### Scenario: Staff without the notification permission

- **WHEN** a report is filed and an online player does not hold the notification permission
- **THEN** that player does not receive the notification

#### Scenario: Reporter is not notified as staff

- **WHEN** a staff member with notifications enabled files a report
- **THEN** that staff member receives the submission confirmation message
- **AND** that staff member does not additionally receive the staff notification for their own report

### Requirement: Clickable teleport in the notification

The report notification SHALL include a clickable component that, when activated, moves the staff member to the reported player, transferring them to the target's server first when the target is on a different backend server. The component SHALL expose a hover description explaining the action.

#### Scenario: Staff clicks the notification

- **WHEN** a staff member on `survival-2` clicks the teleport component of a notification for a target on `survival-1`
- **THEN** the staff member is transferred to `survival-1`
- **AND** the staff member is teleported to the target's location on arrival

#### Scenario: Staff clicks for a target on their own server

- **WHEN** a staff member clicks the teleport component for a target online on the staff member's own server
- **THEN** the staff member is teleported to the target's location

#### Scenario: Target disconnected before the click

- **WHEN** a staff member clicks the teleport component of a notification whose target is no longer online anywhere on the network
- **THEN** the staff member is not teleported
- **AND** the staff member receives the configured target-offline message

#### Scenario: Report dismissed before the click

- **WHEN** a staff member clicks the teleport component of a notification whose report has since been dismissed or has expired
- **THEN** the staff member receives the configured report-unavailable message
- **AND** the staff member is not teleported

### Requirement: Notification toggle

The system SHALL provide the command `/togglereport` to players holding the notification permission, switching their notification delivery between enabled and disabled and confirming the resulting state. The setting SHALL persist across disconnects and SHALL apply on every backend server the staff member connects to. New staff members SHALL start in the configured default state.

#### Scenario: Disabling notifications

- **WHEN** a staff member with notifications enabled executes `/togglereport`
- **THEN** the system disables their notifications
- **AND** the staff member receives the configured notifications-disabled message
- **AND** the staff member receives no further report notifications

#### Scenario: Enabling notifications

- **WHEN** a staff member with notifications disabled executes `/togglereport`
- **THEN** the system enables their notifications
- **AND** the staff member receives the configured notifications-enabled message
- **AND** the staff member receives subsequent report notifications

#### Scenario: Setting persists across sessions

- **WHEN** a staff member disables notifications, disconnects, and reconnects
- **THEN** their notifications remain disabled

#### Scenario: Setting follows the staff member across servers

- **WHEN** a staff member disables notifications on `survival-1` and then connects to `survival-2`
- **THEN** their notifications remain disabled on `survival-2`

#### Scenario: First-time staff member uses the default

- **WHEN** a staff member who has never used the toggle comes online
- **THEN** their notification delivery matches the configured default state

#### Scenario: Player lacks permission

- **WHEN** a player without the notification permission executes `/togglereport`
- **THEN** the system does not change any setting
- **AND** the player receives the configured no-permission message
