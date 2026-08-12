package s.reports.paper.scheduling;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class MainThread {

    private final Plugin plugin;

    public MainThread(Plugin plugin) {
        this.plugin = plugin;
    }

    public void run(Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }
}
