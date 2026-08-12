package s.reports.paper.teleport;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingTeleportRegistry {

    public record PendingTeleport(UUID targetId, UUID reportId, Instant expiresAt) {

        public boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }

    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

    public void arm(UUID staffId, UUID targetId, UUID reportId, Instant expiresAt) {
        pending.put(staffId, new PendingTeleport(targetId, reportId, expiresAt));
    }

    public PendingTeleport consume(UUID staffId) {
        return pending.remove(staffId);
    }
}
