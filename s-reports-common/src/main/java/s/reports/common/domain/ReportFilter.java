package s.reports.common.domain;

import java.util.List;
import java.util.Locale;

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
}
