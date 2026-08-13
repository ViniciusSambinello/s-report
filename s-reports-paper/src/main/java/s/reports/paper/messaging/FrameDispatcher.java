package s.reports.paper.messaging;

import java.time.Instant;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import s.reports.common.domain.Report;
import s.reports.common.domain.ReportView;
import s.reports.common.protocol.FrameCodec;
import s.reports.common.protocol.FrameDecodeResult;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.common.protocol.ReportFrame;
import s.reports.paper.cache.ValidReportCache;
import s.reports.paper.notification.NotificationService;
import s.reports.paper.reconciliation.ReconciliationService;
import s.reports.paper.submission.PendingResolveRegistry;
import s.reports.paper.teleport.PendingTeleportRegistry;
import s.reports.paper.teleport.TeleportFlowService;

public final class FrameDispatcher implements PluginMessageListener {

    private final Logger logger;
    private final FrameSender frameSender;
    private final PendingResolveRegistry pendingResolveRegistry;
    private final ValidReportCache cache;
    private final NotificationService notificationService;
    private final TeleportFlowService teleportFlowService;
    private final PendingTeleportRegistry pendingTeleportRegistry;
    private final ReconciliationService reconciliationService;
    private final String exemptPermission;

    public FrameDispatcher(
            Logger logger,
            FrameSender frameSender,
            PendingResolveRegistry pendingResolveRegistry,
            ValidReportCache cache,
            NotificationService notificationService,
            TeleportFlowService teleportFlowService,
            PendingTeleportRegistry pendingTeleportRegistry,
            ReconciliationService reconciliationService,
            String exemptPermission) {
        this.logger = logger;
        this.frameSender = frameSender;
        this.pendingResolveRegistry = pendingResolveRegistry;
        this.cache = cache;
        this.notificationService = notificationService;
        this.teleportFlowService = teleportFlowService;
        this.pendingTeleportRegistry = pendingTeleportRegistry;
        this.reconciliationService = reconciliationService;
        this.exemptPermission = exemptPermission;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!ProtocolChannel.IDENTIFIER.equals(channel)) {
            return;
        }
        final FrameDecodeResult decoded = FrameCodec.decode(message);
        if (decoded instanceof FrameDecodeResult.Rejected rejected) {
            logger.warning("Rejected a malformed s-reports frame: " + rejected.reason());
            return;
        }
        dispatch(((FrameDecodeResult.Accepted) decoded).frame());
    }

    private void dispatch(ReportFrame frame) {
        switch (frame) {
            case ReportFrame.TargetResolveResponse f -> pendingResolveRegistry.complete(f);
            case ReportFrame.TargetProbe f -> handleProbe(f);
            case ReportFrame.ReportCreated f -> handleReportCreated(f);
            case ReportFrame.ReportDismissed f -> cache.remove(f.reportId());
            case ReportFrame.TeleportArm f -> pendingTeleportRegistry.arm(
                    f.staffId(), f.targetId(), f.reportId(), Instant.ofEpochMilli(f.expiresAtEpochMilli()));
            case ReportFrame.TeleportGrant f -> teleportFlowService.handleGrant(f);
            case ReportFrame.TeleportDenied f -> teleportFlowService.handleDenied(f);
            case ReportFrame.SyncRequest f -> reconciliationService.reconcile();
            case ReportFrame.TargetResolveRequest f -> logUnexpected("TargetResolveRequest");
            case ReportFrame.TargetProbeResult f -> logUnexpected("TargetProbeResult");
            case ReportFrame.TeleportRequest f -> logUnexpected("TeleportRequest");
        }
    }

    private void handleProbe(ReportFrame.TargetProbe probe) {
        final Player target = Bukkit.getPlayer(probe.targetId());
        if (target == null) {
            return;
        }
        final boolean exempt = target.hasPermission(exemptPermission);
        frameSender.send(target, new ReportFrame.TargetProbeResult(
                probe.requestId(), probe.originServer(), probe.targetId(), target.getName(), exempt));
    }

    private void handleReportCreated(ReportFrame.ReportCreated frame) {
        final Report report = new Report(
                frame.reportId(),
                frame.targetId(),
                frame.targetName(),
                frame.reporterId(),
                frame.reporterName(),
                frame.reason(),
                frame.originServer(),
                Instant.ofEpochMilli(frame.createdAtEpochMilli()),
                Instant.ofEpochMilli(frame.expiresAtEpochMilli()),
                null,
                null);
        final ReportView view = new ReportView(report, frame.lifetimeReportCount(), null);
        cache.put(view);
        notificationService.notifyNewReport(view, frame.reporterId());
    }

    private void logUnexpected(String frameName) {
        logger.warning("Ignoring unexpected " + frameName + " received on the Paper side");
    }
}
