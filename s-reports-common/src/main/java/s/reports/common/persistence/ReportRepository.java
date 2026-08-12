package s.reports.common.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;
import s.reports.common.domain.Report;
import s.reports.common.domain.ReportView;

public final class ReportRepository extends AbstractRepository {

    public ReportRepository(
            DataSource dataSource, String tablePrefix, RepositoryExecutor executor, DatabaseAvailability availability) {
        super(dataSource, tablePrefix, executor, availability);
    }

    public CompletableFuture<Void> insert(Report report) {
        return execute(connection -> {
            connection.setAutoCommit(false);
            try {
                insertReportRow(connection, report);
                upsertReportCount(connection, report.targetId(), report.targetName(), report.createdAt());
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
            return null;
        });
    }

    public CompletableFuture<List<ReportView>> findValid(Instant now) {
        return execute(connection -> queryValid(connection, now));
    }

    public CompletableFuture<Boolean> dismiss(UUID reportId, UUID dismissedBy, Instant now) {
        return execute(connection -> updateDismissal(connection, reportId, dismissedBy, now));
    }

    public CompletableFuture<Optional<Instant>> lastReportInstant(UUID reporterId) {
        return execute(connection -> queryLastReportInstant(connection, reporterId));
    }

    public CompletableFuture<Boolean> hasValidReportFrom(UUID reporterId, UUID targetId, Instant now) {
        return execute(connection -> queryHasValidReportFrom(connection, reporterId, targetId, now));
    }

    public CompletableFuture<Integer> deleteOlderThan(Duration retentionPeriod, Instant now) {
        if (retentionPeriod.isZero()) {
            return CompletableFuture.completedFuture(0);
        }
        return execute(connection -> deleteOlderThanRows(connection, retentionPeriod, now));
    }

    public CompletableFuture<Long> lifetimeCount(UUID targetId) {
        return execute(connection -> queryLifetimeCount(connection, targetId));
    }

    public CompletableFuture<Optional<Report>> findById(UUID reportId) {
        return execute(connection -> queryById(connection, reportId));
    }

    private void insertReportRow(Connection connection, Report report) throws SQLException {
        final String sql = "INSERT INTO " + tableName("reports")
                + " (report_uuid, target_uuid, target_name, reporter_uuid, reporter_name, reason, origin_server, created_at, expires_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinary.toBytes(report.reportId()));
            statement.setBytes(2, UuidBinary.toBytes(report.targetId()));
            statement.setString(3, report.targetName());
            statement.setBytes(4, UuidBinary.toBytes(report.reporterId()));
            statement.setString(5, report.reporterName());
            statement.setString(6, report.reason());
            statement.setString(7, report.originServer());
            statement.setLong(8, report.createdAt().toEpochMilli());
            statement.setLong(9, report.expiresAt().toEpochMilli());
            statement.executeUpdate();
        }
    }

    private void upsertReportCount(Connection connection, UUID targetId, String targetName, Instant reportedAt) throws SQLException {
        final String sql = "INSERT INTO " + tableName("report_counts")
                + " (player_uuid, player_name, report_count, last_reported) VALUES (?, ?, 1, ?)"
                + " ON DUPLICATE KEY UPDATE player_name = VALUES(player_name),"
                + " report_count = report_count + 1, last_reported = VALUES(last_reported)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinary.toBytes(targetId));
            statement.setString(2, targetName);
            statement.setLong(3, reportedAt.toEpochMilli());
            statement.executeUpdate();
        }
    }

    private List<ReportView> queryValid(Connection connection, Instant now) throws SQLException {
        final String sql = "SELECT r.report_uuid, r.target_uuid, r.target_name, r.reporter_uuid, r.reporter_name,"
                + " r.reason, r.origin_server, r.created_at, r.expires_at, r.dismissed_at, r.dismissed_by,"
                + " COALESCE(c.report_count, 0) AS lifetime_count"
                + " FROM " + tableName("reports") + " r"
                + " LEFT JOIN " + tableName("report_counts") + " c ON c.player_uuid = r.target_uuid"
                + " WHERE r.dismissed_at IS NULL AND r.expires_at > ?"
                + " ORDER BY r.created_at DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now.toEpochMilli());
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<ReportView> views = new ArrayList<>();
                while (resultSet.next()) {
                    views.add(new ReportView(readReport(resultSet), resultSet.getLong("lifetime_count"), null));
                }
                return List.copyOf(views);
            }
        }
    }

    private boolean updateDismissal(Connection connection, UUID reportId, UUID dismissedBy, Instant now) throws SQLException {
        final String sql = "UPDATE " + tableName("reports")
                + " SET dismissed_at = ?, dismissed_by = ?"
                + " WHERE report_uuid = ? AND dismissed_at IS NULL AND expires_at > ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now.toEpochMilli());
            statement.setBytes(2, UuidBinary.toBytes(dismissedBy));
            statement.setBytes(3, UuidBinary.toBytes(reportId));
            statement.setLong(4, now.toEpochMilli());
            return statement.executeUpdate() > 0;
        }
    }

    private Optional<Instant> queryLastReportInstant(Connection connection, UUID reporterId) throws SQLException {
        final String sql = "SELECT MAX(created_at) FROM " + tableName("reports") + " WHERE reporter_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinary.toBytes(reporterId));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                final long value = resultSet.getLong(1);
                return resultSet.wasNull() ? Optional.empty() : Optional.of(Instant.ofEpochMilli(value));
            }
        }
    }

    private boolean queryHasValidReportFrom(Connection connection, UUID reporterId, UUID targetId, Instant now) throws SQLException {
        final String sql = "SELECT 1 FROM " + tableName("reports")
                + " WHERE reporter_uuid = ? AND target_uuid = ? AND dismissed_at IS NULL AND expires_at > ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinary.toBytes(reporterId));
            statement.setBytes(2, UuidBinary.toBytes(targetId));
            statement.setLong(3, now.toEpochMilli());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Optional<Report> queryById(Connection connection, UUID reportId) throws SQLException {
        final String sql = "SELECT report_uuid, target_uuid, target_name, reporter_uuid, reporter_name, reason,"
                + " origin_server, created_at, expires_at, dismissed_at, dismissed_by"
                + " FROM " + tableName("reports") + " WHERE report_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinary.toBytes(reportId));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readReport(resultSet)) : Optional.empty();
            }
        }
    }

    private long queryLifetimeCount(Connection connection, UUID targetId) throws SQLException {
        final String sql = "SELECT report_count FROM " + tableName("report_counts") + " WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinary.toBytes(targetId));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private int deleteOlderThanRows(Connection connection, Duration retentionPeriod, Instant now) throws SQLException {
        final long cutoff = now.minus(retentionPeriod).toEpochMilli();
        final String sql = "DELETE FROM " + tableName("reports") + " WHERE created_at < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cutoff);
            return statement.executeUpdate();
        }
    }

    private Report readReport(ResultSet resultSet) throws SQLException {
        final long dismissedAtValue = resultSet.getLong("dismissed_at");
        final Instant dismissedAt = resultSet.wasNull() ? null : Instant.ofEpochMilli(dismissedAtValue);
        final byte[] dismissedByBytes = resultSet.getBytes("dismissed_by");
        final UUID dismissedBy = dismissedByBytes == null ? null : UuidBinary.fromBytes(dismissedByBytes);
        return new Report(
                UuidBinary.fromBytes(resultSet.getBytes("report_uuid")),
                UuidBinary.fromBytes(resultSet.getBytes("target_uuid")),
                resultSet.getString("target_name"),
                UuidBinary.fromBytes(resultSet.getBytes("reporter_uuid")),
                resultSet.getString("reporter_name"),
                resultSet.getString("reason"),
                resultSet.getString("origin_server"),
                Instant.ofEpochMilli(resultSet.getLong("created_at")),
                Instant.ofEpochMilli(resultSet.getLong("expires_at")),
                dismissedAt,
                dismissedBy);
    }
}
