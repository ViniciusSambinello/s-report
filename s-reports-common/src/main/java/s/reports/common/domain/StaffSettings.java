package s.reports.common.domain;

import java.util.Objects;
import java.util.UUID;

public record StaffSettings(UUID playerId, boolean notificationsEnabled) {

    public StaffSettings {
        Objects.requireNonNull(playerId, "playerId");
    }
}
