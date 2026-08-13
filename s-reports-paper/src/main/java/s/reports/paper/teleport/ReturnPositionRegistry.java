package s.reports.paper.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;

public final class ReturnPositionRegistry {

    public record ReturnPosition(String server, Location location) {
    }

    private final Map<UUID, ReturnPosition> positions = new ConcurrentHashMap<>();

    public void record(UUID staffId, String server, Location location) {
        positions.put(staffId, new ReturnPosition(server, location.clone()));
    }

    public ReturnPosition get(UUID staffId) {
        return positions.get(staffId);
    }

    public void clear(UUID staffId) {
        positions.remove(staffId);
    }
}
