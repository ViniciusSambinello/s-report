package s.reports.common.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportFilterTest {

    private Report reportWithReason(String reason) {
        final Instant now = Instant.now();
        return new Report(
                UUID.randomUUID(), UUID.randomUUID(), "Target", UUID.randomUUID(), "Reporter",
                reason, "server", now, now.plusSeconds(3600), null, null);
    }

    private Report reportWithTarget(UUID targetId) {
        final Instant now = Instant.now();
        return new Report(
                UUID.randomUUID(), targetId, "Target", UUID.randomUUID(), "Reporter",
                "reason", "server", now, now.plusSeconds(3600), null, null);
    }

    private ReportView viewWith(Instant expiresAt, Instant dismissedAt) {
        final Instant now = Instant.now();
        final Report report = new Report(
                UUID.randomUUID(), UUID.randomUUID(), "Target", UUID.randomUUID(), "Reporter",
                "reason", "server", now, expiresAt, dismissedAt, dismissedAt == null ? null : UUID.randomUUID());
        return new ReportView(report, 1L, null);
    }

    @Test
    void caseInsensitiveMatch() {
        final List<Report> reports = List.of(reportWithReason("flying in spawn"));
        assertEquals(1, ReportFilter.matching(reports, "FLY").size());
    }

    @Test
    void substringMatch() {
        final List<Report> reports = List.of(
                reportWithReason("flying in spawn"),
                reportWithReason("using fly hack"),
                reportWithReason("griefing my base"));
        assertEquals(2, ReportFilter.matching(reports, "fly").size());
    }

    @Test
    void multiWordMatch() {
        final List<Report> reports = List.of(reportWithReason("he is using kill aura"));
        assertEquals(1, ReportFilter.matching(reports, "kill aura").size());
    }

    @Test
    void noMatchReturnsEmpty() {
        final List<Report> reports = List.of(reportWithReason("griefing my base"));
        assertTrue(ReportFilter.matching(reports, "xyz").isEmpty());
    }

    @Test
    void blankKeywordReturnsEverything() {
        final List<Report> reports = List.of(reportWithReason("a"), reportWithReason("b"));
        assertEquals(2, ReportFilter.matching(reports, "").size());
    }

    @Test
    void excludingExpiredKeepsOnlyValidViews() {
        final Instant now = Instant.now();
        final ReportView valid = viewWith(now.plusSeconds(3600), null);
        final ReportView expired = viewWith(now.minusSeconds(1), null);
        final ReportView dismissed = viewWith(now.plusSeconds(3600), now);

        final List<ReportView> remaining = ReportFilter.excludingExpired(List.of(valid, expired, dismissed), now);

        assertEquals(1, remaining.size());
        assertEquals(valid.report().reportId(), remaining.get(0).report().reportId());
    }

    @Test
    void excludingExpiredKeepsEverythingWhenAllValid() {
        final Instant now = Instant.now();
        final List<ReportView> views = List.of(viewWith(now.plusSeconds(60), null), viewWith(now.plusSeconds(120), null));

        assertEquals(2, ReportFilter.excludingExpired(views, now).size());
    }

    @Test
    void viewsWithValidCountsGroupsPerTarget() {
        final UUID targetA = UUID.randomUUID();
        final UUID targetB = UUID.randomUUID();
        final Report firstForA = reportWithTarget(targetA);
        final Report secondForA = reportWithTarget(targetA);
        final Report onlyForB = reportWithTarget(targetB);

        final List<ReportView> views = ReportFilter.viewsWithValidCounts(List.of(firstForA, secondForA, onlyForB));

        final ReportView firstView = views.stream().filter(v -> v.report().reportId().equals(firstForA.reportId())).findFirst().orElseThrow();
        final ReportView secondView = views.stream().filter(v -> v.report().reportId().equals(secondForA.reportId())).findFirst().orElseThrow();
        final ReportView bView = views.stream().filter(v -> v.report().reportId().equals(onlyForB.reportId())).findFirst().orElseThrow();

        assertEquals(2L, firstView.reportCount());
        assertEquals(2L, secondView.reportCount());
        assertEquals(1L, bView.reportCount());
    }
}
