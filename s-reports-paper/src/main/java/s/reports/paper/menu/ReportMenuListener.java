package s.reports.paper.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import s.reports.common.domain.ReportView;
import s.reports.paper.teleport.TeleportFlowService;

public final class ReportMenuListener implements Listener {

    private final ReportMenuService menuService;
    private final TeleportFlowService teleportFlowService;

    public ReportMenuListener(ReportMenuService menuService, TeleportFlowService teleportFlowService) {
        this.menuService = menuService;
        this.teleportFlowService = teleportFlowService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReportMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        final int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        final var menuConfig = menuService.menuConfig();
        if (slot == menuConfig.previousPageSlot()) {
            menuService.changePage(holder, -1);
            return;
        }
        if (slot == menuConfig.nextPageSlot()) {
            menuService.changePage(holder, 1);
            return;
        }
        final int entryIndex = menuConfig.entrySlots().indexOf(slot);
        if (entryIndex < 0) {
            return;
        }
        final int absoluteIndex = holder.page() * menuConfig.entrySlots().size() + entryIndex;
        if (absoluteIndex < 0 || absoluteIndex >= holder.reports().size()) {
            return;
        }
        final ReportView view = holder.reports().get(absoluteIndex);
        if (event.getClick().isLeftClick()) {
            player.closeInventory();
            teleportFlowService.requestTeleport(player, view.report().reportId());
        } else if (event.getClick().isRightClick()) {
            menuService.dismiss(player, holder, view.report().reportId());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ReportMenuHolder holder) {
            menuService.untrack(holder);
        }
    }
}
