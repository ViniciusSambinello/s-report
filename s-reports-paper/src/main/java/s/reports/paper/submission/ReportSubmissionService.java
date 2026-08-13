package s.reports.paper.submission;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import s.reports.common.domain.Report;
import s.reports.common.domain.ReportView;
import s.reports.common.persistence.ReportRepository;
import s.reports.common.protocol.ReportFrame;
import s.reports.paper.cache.ValidReportCache;
import s.reports.paper.config.PaperConfig;
import s.reports.paper.message.MessageService;
import s.reports.paper.messaging.FrameSender;
import s.reports.paper.notification.NotificationService;
import s.reports.paper.scheduling.MainThread;

public final class ReportSubmissionService {

    private final PaperConfig config;
    private final ReportRepository reportRepository;
    private final MessageService messageService;
    private final FrameSender frameSender;
    private final PendingResolveRegistry pendingResolveRegistry;
    private final MainThread mainThread;
    private final ValidReportCache cache;
    private final NotificationService notificationService;

    public ReportSubmissionService(
            PaperConfig config,
            ReportRepository reportRepository,
            MessageService messageService,
            FrameSender frameSender,
            PendingResolveRegistry pendingResolveRegistry,
            MainThread mainThread,
            ValidReportCache cache,
            NotificationService notificationService) {
        this.config = config;
        this.reportRepository = reportRepository;
        this.messageService = messageService;
        this.frameSender = frameSender;
        this.pendingResolveRegistry = pendingResolveRegistry;
        this.mainThread = mainThread;
        this.cache = cache;
        this.notificationService = notificationService;
    }

    public void submit(Player reporter, String targetName, String reason) {
        if (!reporter.hasPermission(config.permissions().report())) {
            messageService.send(reporter, "no-permission");
            return;
        }
        if (reason == null || reason.isBlank()) {
            messageService.send(reporter, "usage-report");
            return;
        }
        final String trimmedReason = reason.strip();
        if (trimmedReason.length() < config.behaviour().reasonMinLength()) {
            messageService.send(reporter, "reason-too-short",
                    Map.of("min", String.valueOf(config.behaviour().reasonMinLength())));
            return;
        }
        if (trimmedReason.length() > config.behaviour().reasonMaxLength()) {
            messageService.send(reporter, "reason-too-long",
                    Map.of("max", String.valueOf(config.behaviour().reasonMaxLength())));
            return;
        }
        if (reporter.hasPermission(config.permissions().cooldownBypass())) {
            resolveAndSubmit(reporter, targetName, trimmedReason);
            return;
        }
        checkCooldown(reporter, targetName, trimmedReason);
    }

    private void checkCooldown(Player reporter, String targetName, String reason) {
        reportRepository.lastReportInstant(reporter.getUniqueId()).whenComplete((lastReport, throwable) -> mainThread.run(() -> {
            if (throwable != null) {
                messageService.send(reporter, "storage-unavailable");
                return;
            }
            final Instant now = Instant.now();
            if (lastReport.isPresent()) {
                final Instant readyAt = lastReport.get().plus(config.behaviour().cooldown());
                if (now.isBefore(readyAt)) {
                    final long remaining = Duration.between(now, readyAt).getSeconds() + 1;
                    messageService.send(reporter, "cooldown", Map.of("remaining", String.valueOf(remaining)));
                    return;
                }
            }
            resolveAndSubmit(reporter, targetName, reason);
        }));
    }

    private void resolveAndSubmit(Player reporter, String targetName, String reason) {
        final UUID requestId = UUID.randomUUID();
        final var future = pendingResolveRegistry.register(requestId, config.behaviour().resolveTimeout());
        frameSender.send(reporter, new ReportFrame.TargetResolveRequest(requestId, reporter.getUniqueId(), targetName));
        future.whenComplete((response, throwable) -> mainThread.run(() -> {
            if (throwable != null) {
                messageService.send(reporter, "resolve-timeout");
                return;
            }
            handleResolveResponse(reporter, reason, response);
        }));
    }

    private void handleResolveResponse(Player reporter, String reason, ReportFrame.TargetResolveResponse response) {
        if (!response.found()) {
            messageService.send(reporter, "target-not-found");
            return;
        }
        if (response.targetId().equals(reporter.getUniqueId())) {
            messageService.send(reporter, "cannot-report-self");
            return;
        }
        if (response.exempt()) {
            messageService.send(reporter, "target-exempt");
            return;
        }
        checkDuplicateAndPersist(reporter, response, reason);
    }

    private void checkDuplicateAndPersist(Player reporter, ReportFrame.TargetResolveResponse response, String reason) {
        if (!config.behaviour().duplicateSuppression()) {
            persist(reporter, response, reason);
            return;
        }
        reportRepository.hasValidReportFrom(reporter.getUniqueId(), response.targetId(), Instant.now())
                .whenComplete((duplicate, throwable) -> mainThread.run(() -> {
                    if (throwable != null) {
                        messageService.send(reporter, "storage-unavailable");
                        return;
                    }
                    if (duplicate) {
                        messageService.send(reporter, "duplicate-report");
                        return;
                    }
                    persist(reporter, response, reason);
                }));
    }

    private void persist(Player reporter, ReportFrame.TargetResolveResponse response, String reason) {
        final Instant now = Instant.now();
        final Report report = new Report(
                UUID.randomUUID(),
                response.targetId(),
                response.targetName(),
                reporter.getUniqueId(),
                reporter.getName(),
                reason,
                config.serverName(),
                now,
                now.plus(config.behaviour().reportTtl()),
                null,
                null);
        reportRepository.insert(report).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                mainThread.run(() -> messageService.send(reporter, "storage-unavailable"));
                return;
            }
            reportRepository.lifetimeCount(report.targetId())
                    .whenComplete((count, countThrowable) -> mainThread.run(
                            () -> onPersisted(reporter, report, countThrowable != null ? 1L : count)));
        });
    }

    private void onPersisted(Player reporter, Report report, long lifetimeCount) {
        final ReportView view = new ReportView(report, lifetimeCount, null);
        cache.put(view);
        frameSender.send(reporter, new ReportFrame.ReportCreated(
                report.reportId(),
                report.targetId(),
                report.targetName(),
                report.reporterId(),
                report.reporterName(),
                report.reason(),
                report.originServer(),
                report.createdAt().toEpochMilli(),
                report.expiresAt().toEpochMilli(),
                lifetimeCount));
        messageService.send(reporter, "submission-confirmed", Map.of("target", report.targetName()));
        notificationService.notifyNewReport(view, reporter.getUniqueId());
    }
}
