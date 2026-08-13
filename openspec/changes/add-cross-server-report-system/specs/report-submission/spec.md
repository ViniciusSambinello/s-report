## Purpose

Defines how a player files a report against another player anywhere on the network, including the validation rules applied to the target and reason, and the rate limit that prevents report spam.

## ADDED Requirements

### Requirement: Filing a report

The system SHALL provide the command `/report <target> <reason>` to any player holding the report submission permission. The `<reason>` argument SHALL accept free text spanning the remainder of the command line. On success the system SHALL record the report, make it visible to staff on every backend server, and confirm submission to the reporter.

#### Scenario: Successful submission

- **WHEN** a permitted player executes `/report Steve using kill aura in the arena` and `Steve` is online on any backend server of the network
- **THEN** the system records a report naming the executing player as reporter, `Steve` as target, and `using kill aura in the arena` as reason
- **AND** the report becomes visible to staff on every backend server
- **AND** the reporter receives the configured submission confirmation message

#### Scenario: Missing reason

- **WHEN** a player executes `/report Steve` with no reason text
- **THEN** the system rejects the command
- **AND** the player receives the configured usage message
- **AND** no report is recorded

#### Scenario: Player lacks permission

- **WHEN** a player without the report submission permission executes `/report Steve cheating`
- **THEN** the system rejects the command
- **AND** the player receives the configured no-permission message
- **AND** no report is recorded

### Requirement: Target validation

The system SHALL only accept a report whose target is a player currently online on the network. The system SHALL reject a report where the target resolves to the reporter, and SHALL reject a report whose target holds the report-exempt permission.

#### Scenario: Target is offline

- **WHEN** a player reports a name that matches no player currently online on any backend server
- **THEN** the system rejects the command
- **AND** the reporter receives the configured target-not-found message
- **AND** no report is recorded

#### Scenario: Target is the reporter

- **WHEN** a player executes `/report <their own name> testing`
- **THEN** the system rejects the command
- **AND** the reporter receives the configured cannot-report-self message
- **AND** no report is recorded

#### Scenario: Target is exempt

- **WHEN** a player reports a target that holds the report-exempt permission
- **THEN** the system rejects the command
- **AND** the reporter receives the configured target-exempt message
- **AND** no report is recorded

#### Scenario: Target is on a different server

- **WHEN** a player on `survival-1` reports a player online on `survival-2`
- **THEN** the system accepts the report
- **AND** the recorded report names `survival-2` as the target's server at submission time

### Requirement: Reason validation

The system SHALL enforce a configurable minimum and maximum reason length. The system SHALL reject a reason shorter than the minimum or longer than the maximum, and SHALL reject a reason that contains no visible characters.

#### Scenario: Reason below minimum length

- **WHEN** a player submits a report whose reason is shorter than the configured minimum length
- **THEN** the system rejects the command
- **AND** the reporter receives the configured reason-too-short message including the minimum length
- **AND** no report is recorded

#### Scenario: Reason above maximum length

- **WHEN** a player submits a report whose reason is longer than the configured maximum length
- **THEN** the system rejects the command
- **AND** the reporter receives the configured reason-too-long message including the maximum length
- **AND** no report is recorded

#### Scenario: Reason is only whitespace

- **WHEN** a player submits a report whose reason consists entirely of whitespace
- **THEN** the system rejects the command
- **AND** the reporter receives the configured usage message
- **AND** no report is recorded

### Requirement: Submission cooldown

After a player successfully files a report, the system SHALL prevent that player from filing another report until a configurable cooldown has elapsed, defaulting to 30 seconds. The cooldown SHALL apply per reporter across the whole network, not per backend server. A rejected submission SHALL NOT start or extend the cooldown. Players holding the cooldown-bypass permission SHALL NOT be subject to the cooldown.

#### Scenario: Second report within the cooldown

- **WHEN** a player files a valid report and then files another report before the configured cooldown has elapsed
- **THEN** the system rejects the second command
- **AND** the reporter receives the configured cooldown message including the remaining time
- **AND** the second report is not recorded

#### Scenario: Report after the cooldown has elapsed

- **WHEN** a player files a valid report and then files another report after the configured cooldown has elapsed
- **THEN** the system accepts the second report

#### Scenario: Cooldown follows the player across servers

- **WHEN** a player files a valid report on `survival-1` and then connects to `survival-2` and files another report before the cooldown has elapsed
- **THEN** the system rejects the second command
- **AND** the reporter receives the configured cooldown message including the remaining time

#### Scenario: Rejected submission does not start the cooldown

- **WHEN** a player's report is rejected because the target is offline, and the player immediately files a valid report against an online target
- **THEN** the system accepts the valid report

#### Scenario: Player bypasses the cooldown

- **WHEN** a player holding the cooldown-bypass permission files two valid reports in immediate succession
- **THEN** the system accepts both reports

### Requirement: Duplicate report suppression

When duplicate suppression is enabled in configuration, the system SHALL reject a report from a reporter against a target for whom that same reporter already has a valid, unexpired, undismissed report.

#### Scenario: Duplicate against the same target

- **WHEN** duplicate suppression is enabled and a player files a report against a target for whom they already have a valid report
- **THEN** the system rejects the command
- **AND** the reporter receives the configured duplicate-report message
- **AND** no additional report is recorded

#### Scenario: Duplicate after the earlier report was dismissed

- **WHEN** duplicate suppression is enabled and a player files a report against a target whose earlier report from that same reporter has been dismissed or has expired
- **THEN** the system accepts the report

#### Scenario: Different reporters against the same target

- **WHEN** duplicate suppression is enabled and two different players each file a report against the same target
- **THEN** the system accepts both reports
