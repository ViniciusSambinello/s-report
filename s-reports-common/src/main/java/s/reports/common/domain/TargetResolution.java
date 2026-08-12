package s.reports.common.domain;

import java.util.Objects;
import java.util.UUID;

public record TargetResolution(UUID targetId, String targetName, String currentServer, boolean exempt) {

    public TargetResolution {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(targetName, "targetName");
        Objects.requireNonNull(currentServer, "currentServer");
    }
}
