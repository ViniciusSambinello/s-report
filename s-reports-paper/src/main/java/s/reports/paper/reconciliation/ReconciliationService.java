package s.reports.paper.reconciliation;

import java.time.Instant;
import s.reports.common.logging.LogSink;
import s.reports.common.persistence.ReportRepository;
import s.reports.paper.cache.ValidReportCache;
import s.reports.paper.scheduling.MainThread;

public final class ReconciliationService {

    private final ReportRepository reportRepository;
    private final ValidReportCache cache;
    private final MainThread mainThread;
    private final LogSink logSink;

    public ReconciliationService(ReportRepository reportRepository, ValidReportCache cache, MainThread mainThread, LogSink logSink) {
        this.reportRepository = reportRepository;
        this.cache = cache;
        this.mainThread = mainThread;
        this.logSink = logSink;
    }

    public void reconcile() {
        reportRepository.findValid(Instant.now())
                .whenComplete((views, throwable) -> mainThread.run(() -> {
                    if (throwable != null) {
                        logSink.warn("Reconciliation failed: " + throwable.getMessage());
                        return;
                    }
                    cache.replaceAll(views);
                }));
    }
}
