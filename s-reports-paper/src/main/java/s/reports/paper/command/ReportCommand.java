package s.reports.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import s.reports.paper.submission.ReportSubmissionService;

public final class ReportCommand {

    private ReportCommand() {
    }

    public static void register(JavaPlugin plugin, ReportSubmissionService submissionService) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("report")
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        Bukkit.getOnlinePlayers().forEach(online -> builder.suggest(online.getName()));
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        if (!(context.getSource().getSender() instanceof Player player)) {
                                            return 0;
                                        }
                                        final String targetName = StringArgumentType.getString(context, "target");
                                        submissionService.submit(player, targetName, "");
                                        return 1;
                                    })
                                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                if (!(context.getSource().getSender() instanceof Player player)) {
                                                    return 0;
                                                }
                                                final String targetName = StringArgumentType.getString(context, "target");
                                                final String reason = StringArgumentType.getString(context, "reason");
                                                submissionService.submit(player, targetName, reason);
                                                return 1;
                                            })))
                            .build(),
                    "Report a player");
        });
    }
}
