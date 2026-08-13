package s.reports.paper.cache;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import s.reports.common.domain.ReportView;

public final class ValidReportCache {

    private final Map<UUID, ReportView> reports = new ConcurrentHashMap<>();

    public void replaceAll(List<ReportView> freshReports) {
        reports.clear();
        for (final ReportView view : freshReports) {
            reports.put(view.report().reportId(), view);
        }
    }

    public void put(ReportView view) {
        reports.put(view.report().reportId(), view);
    }

    public void remove(UUID reportId) {
        reports.remove(reportId);
    }

    public List<ReportView> validSnapshot(Instant now) {
        return reports.values().stream()
                .filter(view -> view.report().isValid(now))
                .toList();
    }

    public ReportView get(UUID reportId) {
        return reports.get(reportId);
    }
}
