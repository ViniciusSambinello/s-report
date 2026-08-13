package s.reports.common.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Report(
        UUID reportId,
        UUID targetId,
        String targetName,
        UUID reporterId,
        String reporterName,
        String reason,
        String originServer,
        Instant createdAt,
        Instant expiresAt,
        Instant dismissedAt,
        UUID dismissedBy) {

    public Report {
        Objects.requireNonNull(reportId, "reportId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(targetName, "targetName");
        Objects.requireNonNull(reporterId, "reporterId");
        Objects.requireNonNull(reporterName, "reporterName");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(originServer, "originServer");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isDismissed() {
        return dismissedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isValid(Instant now) {
        return !isDismissed() && !isExpired(now);
    }
}
