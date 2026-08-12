package s.reports.common.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import s.reports.common.domain.Report;
import s.reports.common.domain.ReportView;
import s.reports.common.logging.LogSink;

@Testcontainers
class ReportRepositoryTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("sreports")
            .withUsername("sreports")
            .withPassword("sreports");

    private static final class NoopLogSink implements LogSink {

        @Override
        public void warn(String message) {
        }

        @Override
        public void severe(String message) {
        }
    }

    private static HikariDataSource dataSource;
    private static RepositoryExecutor executor;
    private static ReportRepository repository;

    @BeforeAll
    static void setUp() throws SQLException {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(MYSQL.getJdbcUrl());
        hikariConfig.setUsername(MYSQL.getUsername());
        hikariConfig.setPassword(MYSQL.getPassword());
        hikariConfig.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(hikariConfig);
        SchemaInitializer.initialize(dataSource, "sreports_");
        executor = new RepositoryExecutor();
        repository = new ReportRepository(dataSource, "sreports_", executor, new DatabaseAvailability(new NoopLogSink()));
    }

    @AfterAll
    static void tearDown() {
        executor.close();
        dataSource.close();
    }

    private Report newReport(UUID targetId, String targetName, UUID reporterId, Duration ttl) {
        final Instant now = Instant.now();
        return new Report(
                UUID.randomUUID(), targetId, targetName, reporterId, "Reporter",
                "reason " + UUID.randomUUID(), "survival-1", now, now.plus(ttl), null, null);
    }

    @Test
    void insertThenFindValidReturnsTheReport() throws Exception {
        final Report report = newReport(UUID.randomUUID(), "Steve", UUID.randomUUID(), Duration.ofHours(1));

        repository.insert(report).get();
        final List<ReportView> valid = repository.findValid(Instant.now()).get();

        assertTrue(valid.stream().anyMatch(view -> view.report().reportId().equals(report.reportId())));
    }

    @Test
    void uuidsRoundTripThroughBinary16() throws Exception {
        final UUID targetId = UUID.randomUUID();
        final UUID reporterId = UUID.randomUUID();
        final Report report = newReport(targetId, "Alex", reporterId, Duration.ofHours(1));

        repository.insert(report).get();
        final Report stored = repository.findById(report.reportId()).get().orElseThrow();

        assertEquals(targetId, stored.targetId());
        assertEquals(reporterId, stored.reporterId());
        assertEquals(report.reportId(), stored.reportId());
    }

    @Test
    void dismissIsIdempotent() throws Exception {
        final Report report = newReport(UUID.randomUUID(), "Steve", UUID.randomUUID(), Duration.ofHours(1));
        repository.insert(report).get();

        final boolean first = repository.dismiss(report.reportId(), UUID.randomUUID(), Instant.now()).get();
        final boolean second = repository.dismiss(report.reportId(), UUID.randomUUID(), Instant.now()).get();

        assertTrue(first);
        assertFalse(second);
    }

    @Test
    void lastReportInstantReflectsTheMostRecentReport() throws Exception {
        final UUID reporterId = UUID.randomUUID();
        repository.insert(newReport(UUID.randomUUID(), "A", reporterId, Duration.ofHours(1))).get();
        final Instant firstInstant = repository.lastReportInstant(reporterId).get().orElseThrow();

        Thread.sleep(10);
        repository.insert(newReport(UUID.randomUUID(), "B", reporterId, Duration.ofHours(1))).get();
        final Instant secondInstant = repository.lastReportInstant(reporterId).get().orElseThrow();

        assertFalse(secondInstant.isBefore(firstInstant));
    }

    @Test
    void hasValidReportFromDetectsDuplicates() throws Exception {
        final UUID reporterId = UUID.randomUUID();
        final UUID targetId = UUID.randomUUID();
        repository.insert(newReport(targetId, "Steve", reporterId, Duration.ofHours(1))).get();

        assertTrue(repository.hasValidReportFrom(reporterId, targetId, Instant.now()).get());
    }

    @Test
    void deleteOlderThanKeepsRecentReports() throws Exception {
        final Report recent = newReport(UUID.randomUUID(), "Steve", UUID.randomUUID(), Duration.ofHours(1));
        repository.insert(recent).get();

        final int deleted = repository.deleteOlderThan(Duration.ofDays(90), Instant.now()).get();

        assertEquals(0, deleted);
        assertTrue(repository.findById(recent.reportId()).get().isPresent());
    }

    @Test
    void retentionOfZeroIsANoOp() throws Exception {
        assertEquals(0, repository.deleteOlderThan(Duration.ZERO, Instant.now()).get());
    }

    @Test
    void lifetimeCountSurvivesASimulatedPlayerRename() throws Exception {
        final UUID targetId = UUID.randomUUID();
        repository.insert(newReport(targetId, "OldName", UUID.randomUUID(), Duration.ofHours(1))).get();
        repository.insert(newReport(targetId, "NewName", UUID.randomUUID(), Duration.ofHours(1))).get();

        assertEquals(2L, repository.lifetimeCount(targetId).get());
    }
}
