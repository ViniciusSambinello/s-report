package s.reports.common.config;

import java.time.Duration;

public sealed interface DurationParseResult permits DurationParseResult.Success, DurationParseResult.Failure {

    record Success(Duration duration) implements DurationParseResult {
    }

    record Failure(String reason) implements DurationParseResult {
    }
}
