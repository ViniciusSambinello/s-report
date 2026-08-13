package s.reports.velocity.config;

import java.time.Duration;
import java.util.Objects;
import s.reports.common.config.ConfigAccessor;
import s.reports.common.config.DatabaseConfig;

public record VelocityConfig(DatabaseConfig database, Duration retentionPeriod, Duration retentionInterval) {

    public VelocityConfig {
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(retentionPeriod, "retentionPeriod");
        Objects.requireNonNull(retentionInterval, "retentionInterval");
    }

    public static VelocityConfig fromRoot(ConfigAccessor root) {
        return new VelocityConfig(
                DatabaseConfig.fromSection(root.section("database")),
                root.getDuration("retention-period", Duration.ofDays(90)),
                root.getDuration("retention-interval", Duration.ofHours(1)));
    }
}
