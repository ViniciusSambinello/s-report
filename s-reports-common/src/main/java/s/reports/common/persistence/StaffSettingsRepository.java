package s.reports.common.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;
import s.reports.common.domain.StaffSettings;

public final class StaffSettingsRepository extends AbstractRepository {

    public StaffSettingsRepository(
            DataSource dataSource, String tablePrefix, RepositoryExecutor executor, DatabaseAvailability availability) {
        super(dataSource, tablePrefix, executor, availability);
    }

    public CompletableFuture<Optional<StaffSettings>> find(UUID playerId) {
        return execute(connection -> queryFind(connection, playerId));
    }

    public CompletableFuture<Void> upsert(StaffSettings settings, Instant now) {
        return execute(connection -> {
            persistUpsert(connection, settings, now);
            return null;
        });
    }

    private Optional<StaffSettings> queryFind(Connection connection, UUID playerId) throws SQLException {
        final String sql = "SELECT notifications_enabled FROM " + tableName("staff_settings") + " WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinary.toBytes(playerId));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new StaffSettings(playerId, resultSet.getBoolean(1)));
            }
        }
    }

    private void persistUpsert(Connection connection, StaffSettings settings, Instant now) throws SQLException {
        final String sql = "INSERT INTO " + tableName("staff_settings")
                + " (player_uuid, notifications_enabled, updated_at) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE notifications_enabled = VALUES(notifications_enabled), updated_at = VALUES(updated_at)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinary.toBytes(settings.playerId()));
            statement.setBoolean(2, settings.notificationsEnabled());
            statement.setLong(3, now.toEpochMilli());
            statement.executeUpdate();
        }
    }
}
