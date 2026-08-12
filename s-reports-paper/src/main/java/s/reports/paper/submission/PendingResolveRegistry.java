package s.reports.paper.submission;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import s.reports.common.protocol.ReportFrame;

public final class PendingResolveRegistry {

    private final Map<UUID, CompletableFuture<ReportFrame.TargetResolveResponse>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<ReportFrame.TargetResolveResponse> register(UUID requestId, Duration timeout) {
        final CompletableFuture<ReportFrame.TargetResolveResponse> future = new CompletableFuture<>();
        pending.put(requestId, future);
        future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        future.whenComplete((response, throwable) -> pending.remove(requestId));
        return future;
    }

    public void complete(ReportFrame.TargetResolveResponse response) {
        final CompletableFuture<ReportFrame.TargetResolveResponse> future = pending.get(response.requestId());
        if (future != null) {
            future.complete(response);
        }
    }
}
