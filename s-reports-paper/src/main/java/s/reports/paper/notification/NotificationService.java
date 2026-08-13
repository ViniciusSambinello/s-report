package s.reports.paper.notification;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import s.reports.common.config.PermissionConfig;
import s.reports.common.domain.ReportView;
import s.reports.paper.message.MessageService;

public final class NotificationService {

    private final PermissionConfig permissions;
    private final StaffSettingsCache staffSettingsCache;
    private final MessageService messageService;

    public NotificationService(PermissionConfig permissions, StaffSettingsCache staffSettingsCache, MessageService messageService) {
        this.permissions = permissions;
        this.staffSettingsCache = staffSettingsCache;
        this.messageService = messageService;
    }

    public void notifyNewReport(ReportView view, UUID excludedPlayerId) {
        final Map<String, String> placeholders = Map.of(
                "target", view.report().targetName(),
                "reporter", view.report().reporterName(),
                "reason", view.report().reason(),
                "server", view.report().originServer(),
                "report_id", view.report().reportId().toString());
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(excludedPlayerId)) {
                continue;
            }
            if (!player.hasPermission(permissions.notifyPermission())) {
                continue;
            }
            if (!staffSettingsCache.isEnabled(player.getUniqueId())) {
                continue;
            }
            messageService.send(player, "notification-format", placeholders);
        }
    }
}
