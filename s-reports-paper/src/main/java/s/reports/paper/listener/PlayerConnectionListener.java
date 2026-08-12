package s.reports.paper.listener;

import java.time.Instant;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import s.reports.paper.message.MessageService;
import s.reports.paper.notification.StaffSettingsCache;
import s.reports.paper.reconciliation.ReconciliationService;
import s.reports.paper.teleport.PendingTeleportRegistry;

public final class PlayerConnectionListener implements Listener {

    private final ReconciliationService reconciliationService;
    private final StaffSettingsCache staffSettingsCache;
    private final PendingTeleportRegistry pendingTeleportRegistry;
    private final MessageService messageService;

    public PlayerConnectionListener(
            ReconciliationService reconciliationService,
            StaffSettingsCache staffSettingsCache,
            PendingTeleportRegistry pendingTeleportRegistry,
            MessageService messageService) {
        this.reconciliationService = reconciliationService;
        this.staffSettingsCache = staffSettingsCache;
        this.pendingTeleportRegistry = pendingTeleportRegistry;
        this.messageService = messageService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (Bukkit.getOnlinePlayers().size() == 1) {
            reconciliationService.reconcile();
        }
        staffSettingsCache.loadOnJoin(player.getUniqueId());
        completePendingTeleport(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        staffSettingsCache.forget(event.getPlayer().getUniqueId());
    }

    private void completePendingTeleport(Player player) {
        final PendingTeleportRegistry.PendingTeleport pending = pendingTeleportRegistry.consume(player.getUniqueId());
        if (pending == null || pending.isExpired(Instant.now())) {
            return;
        }
        final Player target = Bukkit.getPlayer(pending.targetId());
        if (target == null) {
            messageService.send(player, "target-offline");
            return;
        }
        player.teleport(target.getLocation());
        messageService.send(player, "teleport-confirmed", Map.of("target", target.getName()));
    }
}
