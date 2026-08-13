package s.reports.paper.config;

import java.util.Objects;
import s.reports.common.config.BehaviourConfig;
import s.reports.common.config.ConfigAccessor;
import s.reports.common.config.DatabaseConfig;
import s.reports.common.config.PermissionConfig;

public record PaperConfig(String serverName, DatabaseConfig database, BehaviourConfig behaviour, PermissionConfig permissions) {

    public PaperConfig {
        Objects.requireNonNull(serverName, "serverName");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(behaviour, "behaviour");
        Objects.requireNonNull(permissions, "permissions");
    }

    public static PaperConfig fromRoot(ConfigAccessor root) {
        return new PaperConfig(
                root.getString("server-name", "server"),
                DatabaseConfig.fromSection(root.section("database")),
                BehaviourConfig.fromSection(root.section("behaviour")),
                PermissionConfig.fromSection(root.section("permissions")));
    }
}
