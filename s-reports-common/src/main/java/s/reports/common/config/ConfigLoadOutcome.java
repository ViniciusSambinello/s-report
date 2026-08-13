package s.reports.common.config;

public sealed interface ConfigLoadOutcome<T> permits ConfigLoadOutcome.Ready, ConfigLoadOutcome.Failed {

    record Ready<T>(T value) implements ConfigLoadOutcome<T> {
    }

    record Failed<T>(String fileName, String reason) implements ConfigLoadOutcome<T> {
    }
}
