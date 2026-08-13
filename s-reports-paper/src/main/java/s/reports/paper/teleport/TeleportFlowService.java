package s.reports.paper.teleport;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import s.reports.common.domain.Report;
import s.reports.common.domain.TeleportDenyReason;
import s.reports.common.persistence.ReportRepository;
import s.reports.common.protocol.ReportFrame;
import s.reports.paper.config.PaperConfig;
import s.reports.paper.message.MessageService;
import s.reports.paper.messaging.FrameSender;
import s.reports.paper.scheduling.MainThread;

public final class TeleportFlowService {

    private final ReportRepository reportRepository;
    private final FrameSender frameSender;
    private final MessageService messageService;
    private final MainThread mainThread;
    private final ReturnPositionRegistry returnPositionRegistry;
    private final PaperConfig config;

    public TeleportFlowService(
            ReportRepository reportRepository,
            FrameSender frameSender,
            MessageService messageService,
            MainThread mainThread,
            ReturnPositionRegistry returnPositionRegistry,
            PaperConfig config) {
        this.reportRepository = reportRepository;
        this.frameSender = frameSender;
        this.messageService = messageService;
        this.mainThread = mainThread;
        this.returnPositionRegistry = returnPositionRegistry;
        this.config = config;
    }

    public void requestTeleport(Player staff, UUID reportId) {
        reportRepository.findById(reportId).whenComplete((maybeReport, throwable) -> mainThread.run(() -> {
            if (throwable != null) {
                messageService.send(staff, "storage-unavailable");
                return;
            }
            if (maybeReport.isEmpty() || !maybeReport.get().isValid(Instant.now())) {
                messageService.send(staff, "report-unavailable");
                return;
            }
            final Report report = maybeReport.get();
            if (config.behaviour().returnPositionEnabled()) {
                returnPositionRegistry.record(staff.getUniqueId(), config.serverName(), staff.getLocation());
            }
            frameSender.send(staff, new ReportFrame.TeleportRequest(
                    staff.getUniqueId(), reportId, report.targetId(), config.behaviour().pendingTeleportTimeout().toMillis()));
        }));
    }

    public void handleGrant(ReportFrame.TeleportGrant grant) {
        final Player staff = Bukkit.getPlayer(grant.staffId());
        final Player target = Bukkit.getPlayer(grant.targetId());
        if (staff == null || target == null) {
            return;
        }
        staff.teleport(target.getLocation());
        messageService.send(staff, "teleport-confirmed", Map.of("target", target.getName()));
    }

    public void handleDenied(ReportFrame.TeleportDenied denied) {
        final Player staff = Bukkit.getPlayer(denied.staffId());
        if (staff == null) {
            return;
        }
        messageService.send(staff, messageKeyFor(denied.reason()));
    }

    private String messageKeyFor(TeleportDenyReason reason) {
        return switch (reason) {
            case TARGET_OFFLINE -> "target-offline";
            case TARGET_MOVED -> "target-moved";
            case TRANSFER_FAILED -> "transfer-failed";
            case REPORT_UNAVAILABLE -> "report-unavailable";
        };
    }
}
