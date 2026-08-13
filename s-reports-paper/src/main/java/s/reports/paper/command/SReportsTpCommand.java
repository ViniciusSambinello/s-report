package s.reports.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import s.reports.common.config.PermissionConfig;
import s.reports.paper.message.MessageService;
import s.reports.paper.teleport.TeleportFlowService;

public final class SReportsTpCommand {

    private SReportsTpCommand() {
    }

    public static void register(
            JavaPlugin plugin,
            PermissionConfig permissions,
            TeleportFlowService teleportFlowService,
            MessageService messageService) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("sreportstp")
                            .then(Commands.argument("report-id", StringArgumentType.word())
                                    .executes(context -> {
                                        if (!(context.getSource().getSender() instanceof Player player)) {
                                            return 0;
                                        }
                                        if (!player.hasPermission(permissions.notifyPermission())) {
                                            messageService.send(player, "no-permission");
                                            return 0;
                                        }
                                        final UUID reportId;
                                        try {
                                            reportId = UUID.fromString(StringArgumentType.getString(context, "report-id"));
                                        } catch (IllegalArgumentException exception) {
                                            messageService.send(player, "report-unavailable");
                                            return 0;
                                        }
                                        teleportFlowService.requestTeleport(player, reportId);
                                        return 1;
                                    }))
                            .build(),
                    "Teleport to a reported player");
        });
    }
}
