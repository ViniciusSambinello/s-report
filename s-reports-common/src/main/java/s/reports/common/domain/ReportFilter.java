package s.reports.common.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ReportFilter {

    private ReportFilter() {
    }

    public static List<Report> matching(List<Report> reports, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.copyOf(reports);
        }
        final String needle = keyword.toLowerCase(Locale.ROOT);
        return reports.stream()
                .filter(report -> report.reason().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public static List<ReportView> excludingExpired(List<ReportView> views, Instant now) {
        return views.stream().filter(view -> view.report().isValid(now)).toList();
    }

    public static List<ReportView> viewsWithValidCounts(List<Report> validReports) {
        final Map<UUID, Long> countsByTarget = validReports.stream()
                .collect(Collectors.groupingBy(Report::targetId, Collectors.counting()));
        return validReports.stream()
                .map(report -> new ReportView(report, countsByTarget.get(report.targetId()), null))
                .toList();
    }
}
