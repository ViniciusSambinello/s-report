package s.reports.velocity.retention;

import com.velocitypowered.api.proxy.ProxyServer;
import java.time.Duration;
import java.time.Instant;
import s.reports.common.logging.LogSink;
import s.reports.common.persistence.ReportRepository;

public final class RetentionTask {

    private final ProxyServer proxyServer;
    private final Object plugin;
    private final ReportRepository reportRepository;
    private final Duration retentionPeriod;
    private final Duration interval;
    private final LogSink logSink;

    public RetentionTask(
            ProxyServer proxyServer,
            Object plugin,
            ReportRepository reportRepository,
            Duration retentionPeriod,
            Duration interval,
            LogSink logSink) {
        this.proxyServer = proxyServer;
        this.plugin = plugin;
        this.reportRepository = reportRepository;
        this.retentionPeriod = retentionPeriod;
        this.interval = interval;
        this.logSink = logSink;
    }

    public void start() {
        proxyServer.getScheduler()
                .buildTask(plugin, this::run)
                .delay(interval)
                .repeat(interval)
                .schedule();
    }

    private void run() {
        reportRepository.deleteOlderThan(retentionPeriod, Instant.now())
                .exceptionally(throwable -> {
                    logSink.warn("Retention cleanup failed: " + throwable.getMessage());
                    return 0;
                });
    }
}
