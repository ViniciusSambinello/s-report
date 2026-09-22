package s.reports.common.persistence;

import com.mysql.cj.jdbc.Driver;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import s.reports.common.config.DatabaseConfig;

public final class DataSourceFactory {

    private static final Duration LEAK_DETECTION_THRESHOLD = Duration.ofSeconds(60);

    private DataSourceFactory() {
    }

    public static HikariDataSource create(DatabaseConfig config) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setDriverClassName(Driver.class.getName());
        hikariConfig.setUsername(config.user());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.poolSize());
        hikariConfig.setConnectionTimeout(config.connectionTimeout().toMillis());
        hikariConfig.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD.toMillis());
        hikariConfig.setPoolName("s-reports");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        return new HikariDataSource(hikariConfig);
    }
}
