package s.reports.common.config;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import s.reports.common.logging.LogSink;

public final class ConfigAccessor {

    private final Map<String, Object> data;
    private final LogSink logSink;

    public ConfigAccessor(Map<String, Object> data, LogSink logSink) {
        this.data = Objects.requireNonNull(data, "data");
        this.logSink = Objects.requireNonNull(logSink, "logSink");
    }

    public ConfigAccessor section(String key) {
        return new ConfigAccessor(nestedMap(key), logSink);
    }

    public String getString(String key, String defaultValue) {
        final Object raw = data.get(key);
        if (raw == null) {
            logSink.warn("Missing key '" + key + "'; using default '" + defaultValue + "'");
            return defaultValue;
        }
        return String.valueOf(raw);
    }

    public int getInt(String key, int defaultValue) {
        final Object raw = data.get(key);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        logSink.warn("Key '" + key + "' is missing or not a whole number; using default " + defaultValue);
        return defaultValue;
    }

    public int getPositiveInt(String key, int defaultValue) {
        final int value = getInt(key, defaultValue);
        if (value <= 0) {
            logSink.warn("Key '" + key + "' must be a positive number; using default " + defaultValue);
            return defaultValue;
        }
        return value;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        final Object raw = data.get(key);
        if (raw instanceof Boolean bool) {
            return bool;
        }
        logSink.warn("Key '" + key + "' is missing or not a boolean; using default " + defaultValue);
        return defaultValue;
    }

    public Duration getDuration(String key, Duration defaultValue) {
        final Object raw = data.get(key);
        if (raw == null) {
            logSink.warn("Missing duration key '" + key + "'; using default " + defaultValue);
            return defaultValue;
        }
        final DurationParseResult result = DurationParser.parse(String.valueOf(raw));
        if (result instanceof DurationParseResult.Success success) {
            return success.duration();
        }
        final String reason = result instanceof DurationParseResult.Failure failure ? failure.reason() : "is invalid";
        logSink.warn("Key '" + key + "' " + reason + "; using default " + defaultValue);
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(String key) {
        final Object raw = data.get(key);
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
