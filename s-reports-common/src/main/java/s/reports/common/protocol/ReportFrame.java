package s.reports.common.protocol;

import java.util.UUID;
import s.reports.common.domain.TeleportDenyReason;

public sealed interface ReportFrame {

    record TargetResolveRequest(UUID requestId, UUID reporterId, String targetName) implements ReportFrame {
    }

    record TargetProbe(UUID requestId, String originServer, UUID targetId) implements ReportFrame {
    }

    record TargetProbeResult(UUID requestId, String originServer, UUID targetId, String targetName, boolean exempt)
            implements ReportFrame {
    }

    record TargetResolveResponse(
            UUID requestId, boolean found, UUID targetId, String targetName, String currentServer, boolean exempt)
            implements ReportFrame {
    }

    record ReportCreated(
            UUID reportId,
            UUID targetId,
            String targetName,
            UUID reporterId,
            String reporterName,
            String reason,
            String originServer,
            long createdAtEpochMilli,
            long expiresAtEpochMilli) implements ReportFrame {
    }

    record ReportDismissed(UUID reportId, UUID dismissedBy, long dismissedAtEpochMilli) implements ReportFrame {
    }

    record TeleportRequest(UUID staffId, UUID reportId, UUID targetId, long pendingTeleportTimeoutMillis) implements ReportFrame {
    }

    record TeleportArm(UUID staffId, UUID targetId, UUID reportId, long expiresAtEpochMilli) implements ReportFrame {
    }

    record TeleportGrant(UUID staffId, UUID targetId, UUID reportId) implements ReportFrame {
    }

    record TeleportDenied(UUID staffId, UUID reportId, TeleportDenyReason reason) implements ReportFrame {
    }

    record SyncRequest(String serverName) implements ReportFrame {
    }
}
