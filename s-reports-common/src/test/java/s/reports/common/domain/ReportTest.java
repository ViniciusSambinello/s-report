package s.reports.common.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportTest {

    private Report reportWith(Instant expiresAt, Instant dismissedAt) {
        final Instant now = Instant.now();
        return new Report(
                UUID.randomUUID(), UUID.randomUUID(), "Target", UUID.randomUUID(), "Reporter",
                "reason", "server", now, expiresAt, dismissedAt, dismissedAt == null ? null : UUID.randomUUID());
    }

    @Test
    void unexpiredAndUndismissedIsValid() {
        final Report report = reportWith(Instant.now().plus(1, ChronoUnit.HOURS), null);
        assertTrue(report.isValid(Instant.now()));
    }

    @Test
    void expiredIsInvalid() {
        final Report report = reportWith(Instant.now().minus(1, ChronoUnit.MINUTES), null);
        assertFalse(report.isValid(Instant.now()));
    }

    @Test
    void dismissedIsInvalid() {
        final Report report = reportWith(Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());
        assertFalse(report.isValid(Instant.now()));
    }
}
