package s.reports.common.config;

import java.util.Objects;

public record PermissionConfig(
        String report,
        String cooldownBypass,
        String exempt,
        String browse,
        String dismiss,
        String notify) {

    public PermissionConfig {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(cooldownBypass, "cooldownBypass");
        Objects.requireNonNull(exempt, "exempt");
        Objects.requireNonNull(browse, "browse");
        Objects.requireNonNull(dismiss, "dismiss");
        Objects.requireNonNull(notify, "notify");
    }

    public static PermissionConfig fromSection(ConfigAccessor accessor) {
        return new PermissionConfig(
                accessor.getString("report", "sreports.report"),
                accessor.getString("cooldown-bypass", "sreports.cooldown.bypass"),
                accessor.getString("exempt", "sreports.exempt"),
                accessor.getString("browse", "sreports.browse"),
                accessor.getString("dismiss", "sreports.dismiss"),
                accessor.getString("notify", "sreports.notify"));
    }
}
