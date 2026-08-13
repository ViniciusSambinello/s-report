package s.reports.paper.config;

import java.util.List;
import s.reports.common.config.ConfigAccessor;
import s.reports.common.logging.LogSink;

public final class MenuConfigLoader {

    private MenuConfigLoader() {
    }

    public static MenuConfig load(ConfigAccessor root, LogSink logSink) {
        final MenuConfig defaults = MenuConfig.defaultLayout();
        final ConfigAccessor pagination = root.section("pagination");
        final ConfigAccessor emptyState = root.section("empty-state");

        final int rows = clampRows(root.getPositiveInt("rows", defaults.rows()), defaults.rows(), logSink);
        final int size = rows * 9;
        final List<Integer> entrySlots = root.getIntList("entry-slots", defaults.entrySlots());
        final int previousSlot = pagination.getInt("previous-slot", defaults.previousPageSlot());
        final int nextSlot = pagination.getInt("next-slot", defaults.nextPageSlot());
        final int emptySlot = emptyState.getInt("slot", defaults.emptyStateSlot());

        if (!allWithinBounds(size, entrySlots)
                || outOfBounds(size, previousSlot)
                || outOfBounds(size, nextSlot)
                || outOfBounds(size, emptySlot)) {
            logSink.warn("menu.yml contains a slot outside the " + size + "-slot inventory implied by " + rows
                    + " rows; falling back to the default menu layout");
            return defaults;
        }

        return new MenuConfig(
                root.getString("title", defaults.title()),
                rows,
                entrySlots,
                root.getString("entry-material", defaults.entryMaterial()),
                root.getString("entry-name", defaults.entryNameFormat()),
                root.getStringList("entry-lore", defaults.entryLoreFormat()),
                previousSlot,
                pagination.getString("previous-material", defaults.previousPageMaterial()),
                nextSlot,
                pagination.getString("next-material", defaults.nextPageMaterial()),
                emptySlot,
                emptyState.getString("material", defaults.emptyStateMaterial()),
                emptyState.getString("name", defaults.emptyStateName()),
                root.getString("offline-indicator", defaults.offlineIndicator()));
    }

    private static int clampRows(int rows, int fallback, LogSink logSink) {
        if (rows < 1 || rows > 6) {
            logSink.warn("menu.yml 'rows' must be between 1 and 6; using default " + fallback);
            return fallback;
        }
        return rows;
    }

    private static boolean allWithinBounds(int size, List<Integer> slots) {
        return slots.stream().allMatch(slot -> !outOfBounds(size, slot));
    }

    private static boolean outOfBounds(int size, int slot) {
        return slot < 0 || slot >= size;
    }
}
