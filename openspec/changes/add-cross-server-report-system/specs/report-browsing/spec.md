## Purpose

Defines the staff-facing inventory menu that lists valid reports from across the network, the information each entry carries, keyword filtering by reason, and what left-clicking and right-clicking an entry does.

## ADDED Requirements

### Requirement: Opening the report menu

The system SHALL provide the command `/reports` to players holding the report browsing permission, opening an inventory menu that lists every valid report on the network. A report SHALL be considered valid when it has neither expired nor been dismissed. Reports filed on any backend server SHALL appear in the menu regardless of which server the viewing staff member is on.

#### Scenario: Staff opens the menu

- **WHEN** a player holding the report browsing permission executes `/reports`
- **THEN** the system opens an inventory menu listing every valid report on the network

#### Scenario: Reports from other servers are listed

- **WHEN** a report is filed on `survival-1` and a staff member on `survival-2` executes `/reports`
- **THEN** the menu includes the report filed on `survival-1`

#### Scenario: Player lacks permission

- **WHEN** a player without the report browsing permission executes `/reports`
- **THEN** the system does not open the menu
- **AND** the player receives the configured no-permission message

#### Scenario: No valid reports exist

- **WHEN** a staff member executes `/reports` and no valid reports exist on the network
- **THEN** the system opens the menu showing the configured empty-state entry
- **AND** the menu contains no report entries

#### Scenario: Expired and dismissed reports are excluded

- **WHEN** a staff member opens the menu and reports exist that have expired or been dismissed
- **THEN** those reports do not appear in the menu

### Requirement: Live menu refresh

While a staff member has the report menu open, the system SHALL periodically refresh it on a configurable interval. Each refresh SHALL update the time-remaining display on every visible entry and SHALL remove any entry whose report has expired since the menu was opened, without requiring the staff member to close and reopen the menu.

#### Scenario: Time remaining counts down live

- **WHEN** a staff member has the report menu open and the configured refresh interval elapses
- **THEN** the time-remaining display on every visible entry reflects the current countdown

#### Scenario: An entry expires while the menu is open

- **WHEN** a staff member has the report menu open and a listed report expires before the menu is closed
- **THEN** the entry for that report is removed from the menu at the next refresh
- **AND** the staff member is not required to reopen the menu to stop seeing it

#### Scenario: Refresh preserves the current view

- **WHEN** the menu refreshes while a staff member is viewing a page with an active keyword filter
- **THEN** the staff member's current page and active keyword filter remain unchanged after the refresh

### Requirement: Report entry information

Every report entry in the menu SHALL display the target's name, the reporter's name, the report reason, the total number of reports the target has ever received, and the server the target is currently connected to. When the target is no longer online, the entry SHALL display the configured offline indicator in place of the server name.

#### Scenario: Entry shows all required fields

- **WHEN** a staff member views a report entry for target `Steve`, reported by `Alex` for `using kill aura`, where `Steve` has received 7 reports in total and is on `survival-2`
- **THEN** the entry displays the target name `Steve`
- **AND** the entry displays the reporter name `Alex`
- **AND** the entry displays the reason `using kill aura`
- **AND** the entry displays a lifetime report count of 7
- **AND** the entry displays the server `survival-2`

#### Scenario: Target has disconnected

- **WHEN** a staff member views a report entry whose target is no longer online anywhere on the network
- **THEN** the entry displays the configured offline indicator instead of a server name

#### Scenario: Target changed server after the report was filed

- **WHEN** a report is filed while the target is on `survival-1` and the target moves to `survival-2` before a staff member opens the menu
- **THEN** the entry displays `survival-2` as the target's current server

### Requirement: Keyword filtering

The system SHALL provide the command `/reports <keyword>` which opens the report menu restricted to valid reports whose reason contains the given keyword. Matching SHALL be case-insensitive and SHALL match on any substring of the reason.

#### Scenario: Keyword matches some reasons

- **WHEN** a staff member executes `/reports fly` and valid reports exist with reasons `flying in spawn`, `using fly hack`, and `griefing my base`
- **THEN** the menu lists the reports with reasons `flying in spawn` and `using fly hack`
- **AND** the menu does not list the report with reason `griefing my base`

#### Scenario: Keyword matching ignores case

- **WHEN** a staff member executes `/reports FLY` and a valid report exists with reason `flying in spawn`
- **THEN** the menu lists that report

#### Scenario: Keyword matches nothing

- **WHEN** a staff member executes `/reports xyz` and no valid report's reason contains `xyz`
- **THEN** the system opens the menu showing the configured empty-state entry
- **AND** the menu contains no report entries

#### Scenario: Multi-word keyword

- **WHEN** a staff member executes `/reports kill aura` and a valid report exists with reason `he is using kill aura`
- **THEN** the menu lists that report

### Requirement: Menu pagination

When the number of listed reports exceeds the capacity of a single menu page, the system SHALL paginate the listing and provide next-page and previous-page controls. The controls SHALL only be actionable when a page exists in that direction. Pagination SHALL preserve any active keyword filter.

#### Scenario: More reports than fit on one page

- **WHEN** a staff member opens the menu and the number of valid reports exceeds one page
- **THEN** the menu displays the first page of reports
- **AND** the menu displays an actionable next-page control

#### Scenario: Navigating to the next page

- **WHEN** a staff member on the first page activates the next-page control
- **THEN** the menu displays the following page of reports
- **AND** the menu displays an actionable previous-page control

#### Scenario: No further page exists

- **WHEN** a staff member is viewing the last page of reports
- **THEN** the next-page control is not actionable
- **AND** activating it leaves the displayed page unchanged

#### Scenario: Filter survives pagination

- **WHEN** a staff member opens the menu with keyword `fly` and navigates to the next page
- **THEN** the following page lists only reports whose reason contains `fly`

### Requirement: Left-click teleports to the target

Left-clicking a report entry SHALL close the menu and move the staff member to the reported player, transferring them to the target's server first when the target is on a different backend server. The report SHALL remain valid after a teleport.

#### Scenario: Target on the same server

- **WHEN** a staff member left-clicks a report entry whose target is online on the staff member's own server
- **THEN** the menu closes
- **AND** the staff member is teleported to the target's location
- **AND** the staff member receives the configured teleport confirmation message

#### Scenario: Target on a different server

- **WHEN** a staff member on `survival-1` left-clicks a report entry whose target is online on `survival-2`
- **THEN** the menu closes
- **AND** the staff member is transferred to `survival-2`
- **AND** the staff member is teleported to the target's location on arrival

#### Scenario: Target disconnected before the click

- **WHEN** a staff member left-clicks a report entry whose target is no longer online anywhere on the network
- **THEN** the staff member is not teleported
- **AND** the staff member receives the configured target-offline message

#### Scenario: Report stays valid after teleport

- **WHEN** a staff member left-clicks a report entry and is teleported to the target
- **THEN** the report remains listed for other staff members

### Requirement: Right-click dismisses the report

Right-clicking a report entry SHALL dismiss that report, removing it from the valid set on every backend server. The dismissing staff member's menu SHALL refresh to reflect the removal without closing. A dismissed report SHALL remain part of the target's permanent report history. Dismissal SHALL require the report dismissal permission.

#### Scenario: Staff dismisses a report

- **WHEN** a staff member holding the dismissal permission right-clicks a report entry
- **THEN** the report is removed from the valid set
- **AND** the staff member's menu refreshes without the dismissed entry
- **AND** the staff member receives the configured dismissal confirmation message

#### Scenario: Dismissal applies network-wide

- **WHEN** a staff member on `survival-1` dismisses a report and a staff member on `survival-2` opens the menu afterwards
- **THEN** the dismissed report does not appear in the menu on `survival-2`

#### Scenario: Dismissal preserves history

- **WHEN** a report against `Steve` is dismissed and a staff member later views another report entry for `Steve`
- **THEN** the displayed lifetime report count still includes the dismissed report

#### Scenario: Staff lacks the dismissal permission

- **WHEN** a staff member without the dismissal permission right-clicks a report entry
- **THEN** the report is not dismissed
- **AND** the staff member receives the configured no-permission message

#### Scenario: Report already dismissed by another staff member

- **WHEN** a staff member right-clicks a report entry that another staff member has already dismissed
- **THEN** the report is not dismissed a second time
- **AND** the staff member receives the configured report-unavailable message
- **AND** the staff member's menu refreshes without the entry
