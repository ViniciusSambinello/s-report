package s.reports.paper.menu;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import s.reports.common.domain.Report;
import s.reports.common.domain.ReportFilter;
import s.reports.common.domain.ReportView;
import s.reports.common.persistence.ReportRepository;
import s.reports.common.protocol.ReportFrame;
import s.reports.paper.config.MenuConfig;
import s.reports.paper.config.PaperConfig;
import s.reports.paper.message.MessageService;
import s.reports.paper.messaging.FrameSender;
import s.reports.paper.scheduling.MainThread;
import s.reports.paper.submission.PendingResolveRegistry;

public final class ReportMenuService {

    private final MenuConfig menuConfig;
    private final PaperConfig config;
    private final MessageService messageService;
    private final ReportRepository reportRepository;
    private final FrameSender frameSender;
    private final PendingResolveRegistry pendingResolveRegistry;
    private final MainThread mainThread;
    private final Set<ReportMenuHolder> openMenus = new HashSet<>();

    public ReportMenuService(
            MenuConfig menuConfig,
            PaperConfig config,
            MessageService messageService,
            ReportRepository reportRepository,
            FrameSender frameSender,
            PendingResolveRegistry pendingResolveRegistry,
            MainThread mainThread) {
        this.menuConfig = menuConfig;
        this.config = config;
        this.messageService = messageService;
        this.reportRepository = reportRepository;
        this.frameSender = frameSender;
        this.pendingResolveRegistry = pendingResolveRegistry;
        this.mainThread = mainThread;
    }

    public MenuConfig menuConfig() {
        return menuConfig;
    }

    public void open(Player staff, String keyword) {
        if (!staff.hasPermission(config.permissions().browse())) {
            messageService.send(staff, "no-permission");
            return;
        }
        reportRepository.findValid(Instant.now()).whenComplete((valid, throwable) -> {
            if (throwable != null) {
                mainThread.run(() -> messageService.send(staff, "storage-unavailable"));
                return;
            }
            final List<ReportView> filtered = applyFilter(valid, keyword);
            resolveServers(staff, filtered).whenComplete((resolved, resolveThrowable) -> mainThread.run(() -> {
                final ReportMenuHolder holder = new ReportMenuHolder(staff.getUniqueId(), keyword, resolved, 0);
                openPage(holder, staff);
            }));
        });
    }

    public void untrack(ReportMenuHolder holder) {
        openMenus.remove(holder);
    }

    public void refreshOpenMenus(Instant now) {
        for (final ReportMenuHolder holder : openMenus) {
            holder.setReports(ReportFilter.excludingExpired(holder.reports(), now));
            renderPage(holder, holder.getInventory());
        }
    }

    public void changePage(ReportMenuHolder holder, int delta) {
        final int pageSize = menuConfig.entrySlots().size();
        final int totalPages = Math.max(1, (int) Math.ceil(holder.reports().size() / (double) pageSize));
        final int newPage = holder.page() + delta;
        if (newPage < 0 || newPage >= totalPages) {
            return;
        }
        holder.setPage(newPage);
        renderPage(holder, holder.getInventory());
    }

    public void dismiss(Player staff, ReportMenuHolder holder, UUID reportId) {
        if (!staff.hasPermission(config.permissions().dismiss())) {
            messageService.send(staff, "no-permission");
            return;
        }
        reportRepository.dismiss(reportId, staff.getUniqueId(), Instant.now()).whenComplete((changed, throwable) -> mainThread.run(() -> {
            if (throwable != null) {
                messageService.send(staff, "storage-unavailable");
                return;
            }
            if (!changed) {
                messageService.send(staff, "report-unavailable");
            } else {
                messageService.send(staff, "dismiss-confirmed");
                frameSender.send(staff, new ReportFrame.ReportDismissed(reportId, staff.getUniqueId(), Instant.now().toEpochMilli()));
            }
            refreshAfterRemoval(holder, reportId);
        }));
    }

    private void refreshAfterRemoval(ReportMenuHolder holder, UUID reportId) {
        final List<ReportView> updated = holder.reports().stream()
                .filter(view -> !view.report().reportId().equals(reportId))
                .toList();
        holder.setReports(updated);
        renderPage(holder, holder.getInventory());
    }

    private List<ReportView> applyFilter(List<ReportView> valid, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return valid;
        }
        final List<Report> reports = valid.stream().map(ReportView::report).toList();
        final List<Report> matched = ReportFilter.matching(reports, keyword);
        return valid.stream().filter(view -> matched.contains(view.report())).toList();
    }

    private CompletableFuture<List<ReportView>> resolveServers(Player carrier, List<ReportView> views) {
        final List<CompletableFuture<ReportView>> futures = views.stream().map(view -> resolveOne(carrier, view)).toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
    }

    private CompletableFuture<ReportView> resolveOne(Player carrier, ReportView view) {
        final Player local = Bukkit.getPlayer(view.report().targetId());
        if (local != null) {
            return CompletableFuture.completedFuture(new ReportView(view.report(), view.reportCount(), config.serverName()));
        }
        final UUID requestId = UUID.randomUUID();
        final CompletableFuture<ReportFrame.TargetResolveResponse> future =
                pendingResolveRegistry.register(requestId, config.behaviour().resolveTimeout());
        frameSender.send(carrier, new ReportFrame.TargetResolveRequest(requestId, carrier.getUniqueId(), view.report().targetName()));
        return future.handle((response, throwable) -> {
            if (throwable != null || response == null || !response.found()) {
                return view;
            }
            return new ReportView(view.report(), view.reportCount(), response.currentServer());
        });
    }

    private void openPage(ReportMenuHolder holder, Player staff) {
        final Inventory inventory = Bukkit.createInventory(holder, menuConfig.size(), MiniMessage.miniMessage().deserialize(menuConfig.title()));
        holder.setInventory(inventory);
        renderPage(holder, inventory);
        staff.openInventory(inventory);
        openMenus.add(holder);
    }

    private void renderPage(ReportMenuHolder holder, Inventory inventory) {
        inventory.clear();
        final List<ReportView> reports = holder.reports();
        final int pageSize = Math.max(1, menuConfig.entrySlots().size());
        final int totalPages = Math.max(1, (int) Math.ceil(reports.size() / (double) pageSize));
        final int page = Math.min(Math.max(holder.page(), 0), totalPages - 1);
        holder.setPage(page);
        final int fromIndex = Math.min(page * pageSize, reports.size());
        final int toIndex = Math.min(fromIndex + pageSize, reports.size());
        final List<ReportView> pageEntries = reports.subList(fromIndex, toIndex);

        if (pageEntries.isEmpty()) {
            placeItem(inventory, menuConfig.emptyStateSlot(), menuConfig.emptyStateMaterial(), menuConfig.emptyStateName(), Map.of());
        } else {
            for (int index = 0; index < pageEntries.size(); index++) {
                placeEntry(inventory, menuConfig.entrySlots().get(index), pageEntries.get(index));
            }
        }

        placeControl(inventory, menuConfig.previousPageSlot(), menuConfig.previousPageMaterial(), page > 0);
        placeControl(inventory, menuConfig.nextPageSlot(), menuConfig.nextPageMaterial(), page < totalPages - 1);
    }

    private void placeEntry(Inventory inventory, int slot, ReportView view) {
        final Map<String, String> placeholders = entryPlaceholders(view);
        final ItemStack item = new ItemStack(safeMaterial(menuConfig.entryMaterial(), Material.PLAYER_HEAD));
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(renderMenuText(menuConfig.entryNameFormat(), placeholders));
        meta.lore(menuConfig.entryLoreFormat().stream().map(line -> renderMenuText(line, placeholders)).toList());
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void placeItem(Inventory inventory, int slot, String material, String name, Map<String, String> placeholders) {
        final ItemStack item = new ItemStack(safeMaterial(material, Material.BARRIER));
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(renderMenuText(name, placeholders));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void placeControl(Inventory inventory, int slot, String material, boolean actionable) {
        inventory.setItem(slot, actionable ? new ItemStack(safeMaterial(material, Material.ARROW)) : null);
    }

    private Material safeMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private Component renderMenuText(String template, Map<String, String> placeholders) {
        String rendered = template;
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return MiniMessage.miniMessage().deserialize(rendered);
    }

    private Map<String, String> entryPlaceholders(ReportView view) {
        return Map.of(
                "target", view.report().targetName(),
                "reporter", view.report().reporterName(),
                "reason", view.report().reason(),
                "count", String.valueOf(view.reportCount()),
                "server", view.targetOnline() ? view.resolvedServer() : menuConfig.offlineIndicator(),
                "time_remaining", formatRemaining(view.report().expiresAt()));
    }

    private String formatRemaining(Instant expiresAt) {
        final Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative()) {
            return "0s";
        }
        final long totalSeconds = remaining.getSeconds();
        final long hours = totalSeconds / 3600;
        final long minutes = (totalSeconds % 3600) / 60;
        final long seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
