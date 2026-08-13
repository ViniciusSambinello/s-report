package s.reports.common.domain;

import java.util.Objects;

public record ReportView(Report report, long reportCount, String resolvedServer) {

    public ReportView {
        Objects.requireNonNull(report, "report");
    }

    public boolean targetOnline() {
        return resolvedServer != null;
    }
}
