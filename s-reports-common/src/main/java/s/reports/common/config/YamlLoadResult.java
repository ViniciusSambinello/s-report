package s.reports.common.config;

import java.util.Map;

public sealed interface YamlLoadResult permits YamlLoadResult.Loaded, YamlLoadResult.ParseFailure {

    record Loaded(Map<String, Object> root) implements YamlLoadResult {
    }

    record ParseFailure(String fileName, String reason) implements YamlLoadResult {
    }
}
