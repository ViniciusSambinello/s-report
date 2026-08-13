package s.reports.common.persistence;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RepositoryExecutor implements AutoCloseable {

    private final ExecutorService executor;

    public RepositoryExecutor() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    <T> CompletableFuture<T> submit(Callable<T> task) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
