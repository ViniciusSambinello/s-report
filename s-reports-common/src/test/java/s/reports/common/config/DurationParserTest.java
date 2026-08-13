package s.reports.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationParserTest {

    @Test
    void parsesSeconds() {
        final DurationParseResult.Success success =
                assertInstanceOf(DurationParseResult.Success.class, DurationParser.parse("30s"));
        assertEquals(Duration.ofSeconds(30), success.duration());
    }

    @Test
    void parsesMinutes() {
        final DurationParseResult.Success success =
                assertInstanceOf(DurationParseResult.Success.class, DurationParser.parse("45m"));
        assertEquals(Duration.ofMinutes(45), success.duration());
    }

    @Test
    void parsesHours() {
        final DurationParseResult.Success success =
                assertInstanceOf(DurationParseResult.Success.class, DurationParser.parse("1h"));
        assertEquals(Duration.ofHours(1), success.duration());
    }

    @Test
    void parsesDays() {
        final DurationParseResult.Success success =
                assertInstanceOf(DurationParseResult.Success.class, DurationParser.parse("90d"));
        assertEquals(Duration.ofDays(90), success.duration());
    }

    @Test
    void parsesZeroAsABoundaryValue() {
        final DurationParseResult.Success success =
                assertInstanceOf(DurationParseResult.Success.class, DurationParser.parse("0d"));
        assertEquals(Duration.ZERO, success.duration());
    }

    @Test
    void rejectsBlank() {
        assertInstanceOf(DurationParseResult.Failure.class, DurationParser.parse(""));
        assertInstanceOf(DurationParseResult.Failure.class, DurationParser.parse(null));
    }

    @Test
    void rejectsMissingUnit() {
        assertInstanceOf(DurationParseResult.Failure.class, DurationParser.parse("30"));
    }

    @Test
    void rejectsUnknownUnit() {
        assertInstanceOf(DurationParseResult.Failure.class, DurationParser.parse("30x"));
    }

    @Test
    void rejectsNegativeAmount() {
        assertInstanceOf(DurationParseResult.Failure.class, DurationParser.parse("-30s"));
    }

    @Test
    void rejectsNonNumericAmount() {
        assertInstanceOf(DurationParseResult.Failure.class, DurationParser.parse("abcs"));
    }
}
