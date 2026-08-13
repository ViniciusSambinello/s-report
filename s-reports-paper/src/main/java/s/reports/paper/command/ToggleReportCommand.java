package s.reports.paper.command;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import s.reports.common.config.PermissionConfig;
import s.reports.paper.message.MessageService;
import s.reports.paper.notification.StaffSettingsCache;
import s.reports.paper.scheduling.MainThread;

public final class ToggleReportCommand {

    private ToggleReportCommand() {
    }

    public static void register(
            JavaPlugin plugin,
            PermissionConfig permissions,
            StaffSettingsCache staffSettingsCache,
            MessageService messageService,
            MainThread mainThread) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("togglereport")
                            .executes(context -> {
                                if (!(context.getSource().getSender() instanceof Player player)) {
                                    return 0;
                                }
                                if (!player.hasPermission(permissions.notifyPermission())) {
                                    messageService.send(player, "no-permission");
                                    return 0;
                                }
                                staffSettingsCache.toggle(player.getUniqueId())
                                        .whenComplete((enabled, throwable) -> mainThread.run(() -> {
                                            if (throwable != null) {
                                                messageService.send(player, "storage-unavailable");
                                                return;
                                            }
                                            messageService.send(player, enabled ? "notifications-enabled" : "notifications-disabled");
                                        }));
                                return 1;
                            })
                            .build(),
                    "Toggle report notifications");
        });
    }
}
