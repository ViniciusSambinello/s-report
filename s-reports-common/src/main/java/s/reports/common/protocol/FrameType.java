package s.reports.common.protocol;

import java.util.Optional;

public enum FrameType {
    TARGET_RESOLVE_REQUEST,
    TARGET_PROBE,
    TARGET_PROBE_RESULT,
    TARGET_RESOLVE_RESPONSE,
    REPORT_CREATED,
    REPORT_DISMISSED,
    TELEPORT_REQUEST,
    TELEPORT_ARM,
    TELEPORT_GRANT,
    TELEPORT_DENIED,
    SYNC_REQUEST;

    private static final FrameType[] VALUES = values();

    public static Optional<FrameType> fromId(int id) {
        return id >= 0 && id < VALUES.length ? Optional.of(VALUES[id]) : Optional.empty();
    }

    public int id() {
        return ordinal();
    }

    public static FrameType of(ReportFrame frame) {
        return switch (frame) {
            case ReportFrame.TargetResolveRequest f -> TARGET_RESOLVE_REQUEST;
            case ReportFrame.TargetProbe f -> TARGET_PROBE;
            case ReportFrame.TargetProbeResult f -> TARGET_PROBE_RESULT;
            case ReportFrame.TargetResolveResponse f -> TARGET_RESOLVE_RESPONSE;
            case ReportFrame.ReportCreated f -> REPORT_CREATED;
            case ReportFrame.ReportDismissed f -> REPORT_DISMISSED;
            case ReportFrame.TeleportRequest f -> TELEPORT_REQUEST;
            case ReportFrame.TeleportArm f -> TELEPORT_ARM;
            case ReportFrame.TeleportGrant f -> TELEPORT_GRANT;
            case ReportFrame.TeleportDenied f -> TELEPORT_DENIED;
            case ReportFrame.SyncRequest f -> SYNC_REQUEST;
        };
    }
}
