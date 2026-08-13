package s.reports.paper.config;

import java.util.List;
import java.util.Objects;

public record MenuConfig(
        String title,
        int rows,
        List<Integer> entrySlots,
        String entryMaterial,
        String entryNameFormat,
        List<String> entryLoreFormat,
        int previousPageSlot,
        String previousPageMaterial,
        int nextPageSlot,
        String nextPageMaterial,
        int emptyStateSlot,
        String emptyStateMaterial,
        String emptyStateName,
        String offlineIndicator) {

    public MenuConfig {
        Objects.requireNonNull(title, "title");
        entrySlots = List.copyOf(entrySlots);
        entryLoreFormat = List.copyOf(entryLoreFormat);
        Objects.requireNonNull(entryMaterial, "entryMaterial");
        Objects.requireNonNull(entryNameFormat, "entryNameFormat");
        Objects.requireNonNull(previousPageMaterial, "previousPageMaterial");
        Objects.requireNonNull(nextPageMaterial, "nextPageMaterial");
        Objects.requireNonNull(emptyStateMaterial, "emptyStateMaterial");
        Objects.requireNonNull(emptyStateName, "emptyStateName");
        Objects.requireNonNull(offlineIndicator, "offlineIndicator");
    }

    public int size() {
        return rows * 9;
    }

    public static MenuConfig defaultLayout() {
        return new MenuConfig(
                "<dark_gray>Reports</dark_gray>",
                6,
                List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34),
                "PLAYER_HEAD",
                "<white>%target%</white>",
                List.of(
                        "<gray>Reported by <white>%reporter%</white></gray>",
                        "<gray>Reason: <white>%reason%</white></gray>",
                        "<gray>Reports: <white>%count%</white></gray>",
                        "<gray>Server: <white>%server%</white></gray>",
                        "<gray>Expires in: <white>%time_remaining%</white></gray>"),
                48,
                "ARROW",
                50,
                "ARROW",
                22,
                "BARRIER",
                "<gray>No reports found</gray>",
                "<gray>Offline</gray>");
    }
}
