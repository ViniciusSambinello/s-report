package s.reports.common.config;

import java.time.Duration;
import java.util.Objects;

public record BehaviourConfig(
        Duration cooldown,
        Duration reportTtl,
        Duration retentionPeriod,
        Duration reconciliationInterval,
        Duration resolveTimeout,
        Duration pendingTeleportTimeout,
        int reasonMinLength,
        int reasonMaxLength,
        boolean duplicateSuppression,
        boolean returnPositionEnabled,
        boolean defaultNotificationsEnabled) {

    public BehaviourConfig {
        Objects.requireNonNull(cooldown, "cooldown");
        Objects.requireNonNull(reportTtl, "reportTtl");
        Objects.requireNonNull(retentionPeriod, "retentionPeriod");
        Objects.requireNonNull(reconciliationInterval, "reconciliationInterval");
        Objects.requireNonNull(resolveTimeout, "resolveTimeout");
        Objects.requireNonNull(pendingTeleportTimeout, "pendingTeleportTimeout");
    }

    public static BehaviourConfig fromSection(ConfigAccessor accessor) {
        return new BehaviourConfig(
                accessor.getDuration("cooldown", Duration.ofSeconds(30)),
                accessor.getDuration("report-ttl", Duration.ofHours(1)),
                accessor.getDuration("retention-period", Duration.ofDays(90)),
                accessor.getDuration("reconciliation-interval", Duration.ofSeconds(60)),
                accessor.getDuration("resolve-timeout", Duration.ofSeconds(5)),
                accessor.getDuration("pending-teleport-timeout", Duration.ofSeconds(10)),
                accessor.getPositiveInt("reason-min-length", 4),
                accessor.getPositiveInt("reason-max-length", 256),
                accessor.getBoolean("duplicate-suppression", true),
                accessor.getBoolean("return-position", true),
                accessor.getBoolean("default-notifications-enabled", true));
    }
}
