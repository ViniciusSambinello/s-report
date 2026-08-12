package s.reports.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import s.reports.common.config.ConfigAccessor;
import s.reports.common.config.ConfigLoadOutcome;
import s.reports.common.config.YamlDocumentLoader;
import s.reports.common.config.YamlLoadResult;
import s.reports.common.persistence.DataSourceFactory;
import s.reports.common.persistence.DatabaseAvailability;
import s.reports.common.persistence.ReportRepository;
import s.reports.common.persistence.RepositoryExecutor;
import s.reports.common.persistence.SchemaInitializer;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.velocity.broadcast.ReportBroadcastService;
import s.reports.velocity.config.VelocityConfig;
import s.reports.velocity.logging.VelocityLogSink;
import s.reports.velocity.messaging.FrameMessageListener;
import s.reports.velocity.resolution.TargetResolutionService;
import s.reports.velocity.retention.RetentionTask;
import s.reports.velocity.sync.SyncEchoService;
import s.reports.velocity.teleport.TeleportService;

@Plugin(id = "s-reports", name = "s-reports", version = "1.0.0")
public final class SReportsVelocityPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private final VelocityLogSink logSink;

    @Inject
    public SReportsVelocityPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.logSink = new VelocityLogSink(logger);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        proxyServer.getChannelRegistrar().register(MinecraftChannelIdentifier.from(ProtocolChannel.IDENTIFIER));
        final ConfigLoadOutcome<VelocityConfig> outcome = loadConfig();
        if (outcome instanceof ConfigLoadOutcome.Failed<VelocityConfig> failed) {
            logger.error("Failed to parse {}: {}; s-reports will not start", failed.fileName(), failed.reason());
            return;
        }
        final VelocityConfig config = ((ConfigLoadOutcome.Ready<VelocityConfig>) outcome).value();
        start(config);
    }

    private void start(VelocityConfig config) {
        final var dataSource = DataSourceFactory.create(config.database());
        try {
            SchemaInitializer.initialize(dataSource, config.database().tablePrefix());
        } catch (SQLException exception) {
            logger.error("Failed to initialise the database schema: {}; s-reports will not start", exception.getMessage());
            return;
        }
        final RepositoryExecutor repositoryExecutor = new RepositoryExecutor();
        final DatabaseAvailability availability = new DatabaseAvailability(logSink);
        final ReportRepository reportRepository =
                new ReportRepository(dataSource, config.database().tablePrefix(), repositoryExecutor, availability);

        final TargetResolutionService resolutionService = new TargetResolutionService(proxyServer, logger);
        final ReportBroadcastService broadcastService = new ReportBroadcastService(proxyServer, logger);
        final TeleportService teleportService = new TeleportService(proxyServer, logger);
        final SyncEchoService syncEchoService = new SyncEchoService();
        final FrameMessageListener listener =
                new FrameMessageListener(logger, resolutionService, broadcastService, teleportService, syncEchoService);
        proxyServer.getEventManager().register(this, listener);

        new RetentionTask(proxyServer, this, reportRepository, config.retentionPeriod(), config.retentionInterval(), logSink)
                .start();
    }

    private ConfigLoadOutcome<VelocityConfig> loadConfig() {
        final Path configFile = dataDirectory.resolve("config.yml");
        final YamlLoadResult result = YamlDocumentLoader.load(configFile, "config.yml", getClass().getClassLoader());
        if (result instanceof YamlLoadResult.ParseFailure failure) {
            return new ConfigLoadOutcome.Failed<>(failure.fileName(), failure.reason());
        }
        final Map<String, Object> root = ((YamlLoadResult.Loaded) result).root();
        return new ConfigLoadOutcome.Ready<>(VelocityConfig.fromRoot(new ConfigAccessor(root, logSink)));
    }
}
