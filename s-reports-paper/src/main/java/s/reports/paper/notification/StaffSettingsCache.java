package s.reports.paper.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import s.reports.common.domain.StaffSettings;
import s.reports.common.persistence.StaffSettingsRepository;
import s.reports.paper.scheduling.MainThread;

public final class StaffSettingsCache {

    private final Map<UUID, Boolean> enabled = new ConcurrentHashMap<>();
    private final StaffSettingsRepository repository;
    private final boolean defaultEnabled;
    private final MainThread mainThread;

    public StaffSettingsCache(StaffSettingsRepository repository, boolean defaultEnabled, MainThread mainThread) {
        this.repository = repository;
        this.defaultEnabled = defaultEnabled;
        this.mainThread = mainThread;
    }

    public boolean isEnabled(UUID playerId) {
        return enabled.getOrDefault(playerId, defaultEnabled);
    }

    public void loadOnJoin(UUID playerId) {
        repository.find(playerId).whenComplete((settings, throwable) -> mainThread.run(() -> {
            if (throwable != null) {
                enabled.put(playerId, defaultEnabled);
                return;
            }
            enabled.put(playerId, settings.map(StaffSettings::notificationsEnabled).orElse(defaultEnabled));
        }));
    }

    public void forget(UUID playerId) {
        enabled.remove(playerId);
    }

    public CompletableFuture<Boolean> toggle(UUID playerId) {
        final boolean newValue = !isEnabled(playerId);
        return repository.upsert(new StaffSettings(playerId, newValue), Instant.now())
                .thenApply(ignored -> {
                    enabled.put(playerId, newValue);
                    return newValue;
                });
    }
}
