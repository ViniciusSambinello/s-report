package s.reports.common.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import s.reports.common.config.DatabaseConfig;

public final class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static HikariDataSource create(DatabaseConfig config) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setUsername(config.user());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.poolSize());
        hikariConfig.setConnectionTimeout(config.connectionTimeout().toMillis());
        hikariConfig.setPoolName("s-reports");
        return new HikariDataSource(hikariConfig);
    }
}
