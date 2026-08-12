package s.reports.paper.menu;

import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import s.reports.common.domain.ReportView;

public final class ReportMenuHolder implements InventoryHolder {

    private final UUID staffId;
    private final String keyword;
    private List<ReportView> reports;
    private int page;
    private Inventory inventory;

    public ReportMenuHolder(UUID staffId, String keyword, List<ReportView> reports, int page) {
        this.staffId = staffId;
        this.keyword = keyword;
        this.reports = List.copyOf(reports);
        this.page = page;
    }

    public UUID staffId() {
        return staffId;
    }

    public String keyword() {
        return keyword;
    }

    public List<ReportView> reports() {
        return reports;
    }

    public void setReports(List<ReportView> reports) {
        this.reports = List.copyOf(reports);
    }

    public int page() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
