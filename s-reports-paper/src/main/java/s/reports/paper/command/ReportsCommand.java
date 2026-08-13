package s.reports.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import s.reports.paper.menu.ReportMenuService;

public final class ReportsCommand {

    private ReportsCommand() {
    }

    public static void register(JavaPlugin plugin, ReportMenuService menuService) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("reports")
                            .executes(context -> {
                                if (!(context.getSource().getSender() instanceof Player player)) {
                                    return 0;
                                }
                                menuService.open(player, null);
                                return 1;
                            })
                            .then(Commands.argument("keyword", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        if (!(context.getSource().getSender() instanceof Player player)) {
                                            return 0;
                                        }
                                        final String keyword = StringArgumentType.getString(context, "keyword");
                                        menuService.open(player, keyword);
                                        return 1;
                                    }))
                            .build(),
                    "Browse reports");
        });
    }
}
