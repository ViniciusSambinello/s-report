package s.reports.paper.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PendingTeleportRegistryTest {

    @Test
    void armThenConsumeReturnsTheSameEntryKeyedByStaffId() {
        final PendingTeleportRegistry registry = new PendingTeleportRegistry();
        final UUID staffId = UUID.randomUUID();
        final UUID targetId = UUID.randomUUID();
        final UUID reportId = UUID.randomUUID();
        final Instant expiresAt = Instant.now().plusSeconds(30);

        registry.arm(staffId, targetId, reportId, expiresAt);
        final PendingTeleportRegistry.PendingTeleport pending = registry.consume(staffId);

        assertEquals(targetId, pending.targetId());
        assertEquals(reportId, pending.reportId());
        assertEquals(expiresAt, pending.expiresAt());
    }

    @Test
    void consumeRemovesTheEntry() {
        final PendingTeleportRegistry registry = new PendingTeleportRegistry();
        final UUID staffId = UUID.randomUUID();
        registry.arm(staffId, UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(30));

        registry.consume(staffId);

        assertNull(registry.consume(staffId));
    }

    @Test
    void unexpiredEntryIsNotExpired() {
        final PendingTeleportRegistry.PendingTeleport pending = new PendingTeleportRegistry.PendingTeleport(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().plus(1, ChronoUnit.MINUTES));
        assertFalse(pending.isExpired(Instant.now()));
    }

    @Test
    void pastExpiryIsExpired() {
        final PendingTeleportRegistry.PendingTeleport pending = new PendingTeleportRegistry.PendingTeleport(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().minus(1, ChronoUnit.MINUTES));
        assertTrue(pending.isExpired(Instant.now()));
    }

    @Test
    void distinctStaffIdsAreKeyedIndependently() {
        final PendingTeleportRegistry registry = new PendingTeleportRegistry();
        final UUID staffA = UUID.randomUUID();
        final UUID staffB = UUID.randomUUID();
        final UUID targetForA = UUID.randomUUID();
        registry.arm(staffA, targetForA, UUID.randomUUID(), Instant.now().plusSeconds(30));

        assertNull(registry.consume(staffB));
        final PendingTeleportRegistry.PendingTeleport pendingForA = registry.consume(staffA);
        assertEquals(targetForA, pendingForA.targetId());
    }
}
