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
}
