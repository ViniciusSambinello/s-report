package s.reports.paper.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import s.reports.common.protocol.ReportFrame;

class PendingResolveRegistryTest {

    @Test
    void completesWhenTheMatchingResponseArrives() throws Exception {
        final PendingResolveRegistry registry = new PendingResolveRegistry();
        final UUID requestId = UUID.randomUUID();
        final CompletableFuture<ReportFrame.TargetResolveResponse> future = registry.register(requestId, Duration.ofSeconds(5));

        final ReportFrame.TargetResolveResponse response =
                new ReportFrame.TargetResolveResponse(requestId, true, UUID.randomUUID(), "Steve", "survival-1", false);
        registry.complete(response);

        assertEquals(response, future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void expiresAfterTheConfiguredTimeout() {
        final PendingResolveRegistry registry = new PendingResolveRegistry();
        final CompletableFuture<ReportFrame.TargetResolveResponse> future = registry.register(UUID.randomUUID(), Duration.ofMillis(50));

        final ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));
        assertTrue(exception.getCause() instanceof TimeoutException);
    }

    @Test
    void completingAnUnknownRequestIdIsIgnored() {
        final PendingResolveRegistry registry = new PendingResolveRegistry();
        final ReportFrame.TargetResolveResponse response =
                new ReportFrame.TargetResolveResponse(UUID.randomUUID(), true, UUID.randomUUID(), "Steve", "survival-1", false);

        assertDoesNotThrow(() -> registry.complete(response));
    }

    @Test
    void distinctRequestIdsAreKeyedIndependently() throws Exception {
        final PendingResolveRegistry registry = new PendingResolveRegistry();
        final UUID requestIdA = UUID.randomUUID();
        final UUID requestIdB = UUID.randomUUID();
        final CompletableFuture<ReportFrame.TargetResolveResponse> futureA = registry.register(requestIdA, Duration.ofSeconds(5));
        final CompletableFuture<ReportFrame.TargetResolveResponse> futureB = registry.register(requestIdB, Duration.ofSeconds(5));

        final ReportFrame.TargetResolveResponse responseB =
                new ReportFrame.TargetResolveResponse(requestIdB, true, UUID.randomUUID(), "Alex", "survival-2", false);
        registry.complete(responseB);

        assertEquals(responseB, futureB.get(1, TimeUnit.SECONDS));
        assertTrue(!futureA.isDone());
    }
}
