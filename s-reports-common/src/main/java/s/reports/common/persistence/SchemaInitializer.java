package s.reports.common.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class SchemaInitializer {

    private static final String REPORTS_TABLE = """
            CREATE TABLE IF NOT EXISTS `%1$sreports` (
              `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
              `report_uuid`   BINARY(16)      NOT NULL,
              `target_uuid`   BINARY(16)      NOT NULL,
              `target_name`   VARCHAR(16)     NOT NULL,
              `reporter_uuid` BINARY(16)      NOT NULL,
              `reporter_name` VARCHAR(16)     NOT NULL,
              `reason`        VARCHAR(256)    NOT NULL,
              `origin_server` VARCHAR(64)     NOT NULL,
              `created_at`    BIGINT          NOT NULL,
              `expires_at`    BIGINT          NOT NULL,
              `dismissed_at`  BIGINT          NULL,
              `dismissed_by`  BINARY(16)      NULL,
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_report_uuid` (`report_uuid`),
              KEY `idx_valid` (`dismissed_at`, `expires_at`),
              KEY `idx_reporter_created` (`reporter_uuid`, `created_at`),
              KEY `idx_created` (`created_at`)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
            """;

    private static final String REPORT_COUNTS_TABLE = """
            CREATE TABLE IF NOT EXISTS `%1$sreport_counts` (
              `player_uuid`   BINARY(16)   NOT NULL,
              `player_name`   VARCHAR(16)  NOT NULL,
              `report_count`  INT UNSIGNED NOT NULL DEFAULT 0,
              `last_reported` BIGINT       NOT NULL,
              PRIMARY KEY (`player_uuid`)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
            """;

    private static final String STAFF_SETTINGS_TABLE = """
            CREATE TABLE IF NOT EXISTS `%1$sstaff_settings` (
              `player_uuid`           BINARY(16) NOT NULL,
              `notifications_enabled` TINYINT(1) NOT NULL,
              `updated_at`            BIGINT     NOT NULL,
              PRIMARY KEY (`player_uuid`)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
            """;

    private SchemaInitializer() {
    }

    public static void initialize(DataSource dataSource, String tablePrefix) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(REPORTS_TABLE.formatted(tablePrefix));
            statement.executeUpdate(REPORT_COUNTS_TABLE.formatted(tablePrefix));
            statement.executeUpdate(STAFF_SETTINGS_TABLE.formatted(tablePrefix));
        }
    }
}
