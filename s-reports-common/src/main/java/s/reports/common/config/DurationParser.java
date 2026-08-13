package s.reports.common.config;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)([smhd])$");

    private DurationParser() {
    }

    public static DurationParseResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new DurationParseResult.Failure("is blank");
        }
        final Matcher matcher = PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            return new DurationParseResult.Failure(
                    "'" + raw + "' is not a valid duration; expected a whole number followed by s, m, h, or d");
        }
        try {
            final long amount = Long.parseLong(matcher.group(1));
            return new DurationParseResult.Success(toDuration(amount, matcher.group(2)));
        } catch (NumberFormatException exception) {
            return new DurationParseResult.Failure("'" + raw + "' has a number that is out of range");
        }
    }

    private static Duration toDuration(long amount, String unit) {
        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalStateException("Unreachable duration unit " + unit);
        };
    }
}
