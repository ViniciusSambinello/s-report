package s.reports.common.config;

import java.time.Duration;
import java.util.Objects;

public record DatabaseConfig(
        String host,
        int port,
        String database,
        String user,
        String password,
        int poolSize,
        Duration connectionTimeout,
        String tablePrefix) {

    public DatabaseConfig {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(connectionTimeout, "connectionTimeout");
        Objects.requireNonNull(tablePrefix, "tablePrefix");
    }

    public static DatabaseConfig fromSection(ConfigAccessor accessor) {
        return new DatabaseConfig(
                accessor.getString("host", "localhost"),
                accessor.getPositiveInt("port", 3306),
                accessor.getString("database", "sreports"),
                accessor.getString("user", "sreports"),
                accessor.getString("password", ""),
                accessor.getPositiveInt("pool-size", 10),
                accessor.getDuration("connection-timeout", Duration.ofSeconds(30)),
                accessor.getString("table-prefix", "sreports_"));
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database;
    }
}
