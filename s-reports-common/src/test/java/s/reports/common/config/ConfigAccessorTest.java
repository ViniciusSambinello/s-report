package s.reports.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import s.reports.common.logging.LogSink;

class ConfigAccessorTest {

    private static final class RecordingLogSink implements LogSink {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void warn(String message) {
            messages.add(message);
        }

        @Override
        public void severe(String message) {
            messages.add(message);
        }
    }

    @Test
    void missingKeyFallsBackAndLogs() {
        final RecordingLogSink logSink = new RecordingLogSink();
        final ConfigAccessor accessor = new ConfigAccessor(new HashMap<>(), logSink);

        final String value = accessor.getString("missing", "default");

        assertEquals("default", value);
        assertTrue(logSink.messages.stream().anyMatch(message -> message.contains("missing")));
    }

    @Test
    void malformedDurationFallsBackAndLogs() {
        final RecordingLogSink logSink = new RecordingLogSink();
        final Map<String, Object> data = new HashMap<>();
        data.put("ttl", "not-a-duration");
        final ConfigAccessor accessor = new ConfigAccessor(data, logSink);

        final Duration value = accessor.getDuration("ttl", Duration.ofHours(1));

        assertEquals(Duration.ofHours(1), value);
        assertTrue(logSink.messages.stream().anyMatch(message -> message.contains("ttl")));
    }

    @Test
    void outOfRangeIntFallsBackAndLogs() {
        final RecordingLogSink logSink = new RecordingLogSink();
        final Map<String, Object> data = new HashMap<>();
        data.put("pool-size", -5);
        final ConfigAccessor accessor = new ConfigAccessor(data, logSink);

        final int value = accessor.getPositiveInt("pool-size", 10);

        assertEquals(10, value);
        assertTrue(logSink.messages.stream().anyMatch(message -> message.contains("pool-size")));
    }

    @Test
    void validValuesPassThroughWithoutLogging() {
        final RecordingLogSink logSink = new RecordingLogSink();
        final Map<String, Object> data = new HashMap<>();
        data.put("cooldown", "45s");
        final ConfigAccessor accessor = new ConfigAccessor(data, logSink);

        final Duration value = accessor.getDuration("cooldown", Duration.ofSeconds(30));

        assertEquals(Duration.ofSeconds(45), value);
        assertTrue(logSink.messages.isEmpty());
    }
}
