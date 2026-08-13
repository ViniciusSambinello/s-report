package s.reports.paper;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
import s.reports.common.config.ConfigAccessor;
import s.reports.common.config.ConfigLoadOutcome;
import s.reports.common.config.YamlDocumentLoader;
import s.reports.common.config.YamlLoadResult;
import s.reports.common.persistence.DataSourceFactory;
import s.reports.common.persistence.DatabaseAvailability;
import s.reports.common.persistence.ReportRepository;
import s.reports.common.persistence.RepositoryExecutor;
import s.reports.common.persistence.SchemaInitializer;
import s.reports.common.persistence.StaffSettingsRepository;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.paper.cache.ValidReportCache;
import s.reports.paper.command.ReportCommand;
import s.reports.paper.command.ReportsCommand;
import s.reports.paper.command.SReportsTpCommand;
import s.reports.paper.command.ToggleReportCommand;
import s.reports.paper.config.MenuConfig;
import s.reports.paper.config.MenuConfigLoader;
import s.reports.paper.config.PaperConfig;
import s.reports.paper.listener.PlayerConnectionListener;
import s.reports.paper.logging.PaperLogSink;
import s.reports.paper.menu.ReportMenuListener;
import s.reports.paper.menu.ReportMenuService;
import s.reports.paper.message.MessageService;
import s.reports.paper.messaging.FrameDispatcher;
import s.reports.paper.messaging.FrameSender;
import s.reports.paper.notification.NotificationService;
import s.reports.paper.notification.StaffSettingsCache;
import s.reports.paper.reconciliation.ReconciliationService;
import s.reports.paper.scheduling.MainThread;
import s.reports.paper.submission.PendingResolveRegistry;
import s.reports.paper.submission.ReportSubmissionService;
import s.reports.paper.teleport.PendingTeleportRegistry;
import s.reports.paper.teleport.ReturnPositionRegistry;
import s.reports.paper.teleport.TeleportFlowService;

public final class SReportsPaperPlugin extends JavaPlugin {

    private PaperLogSink logSink;
    private HikariDataSource dataSource;
    private RepositoryExecutor repositoryExecutor;

    @Override
    public void onEnable() {
        this.logSink = new PaperLogSink(getLogger());
        final ConfigLoadOutcome<PaperConfig> configOutcome = loadConfig();
        if (configOutcome instanceof ConfigLoadOutcome.Failed<PaperConfig> failed) {
            getLogger().severe("Failed to parse " + failed.fileName() + ": " + failed.reason()
                    + "; s-reports report functionality is disabled");
            return;
        }
        final PaperConfig config = ((ConfigLoadOutcome.Ready<PaperConfig>) configOutcome).value();
        final MenuConfig menuConfig = loadMenuConfig();
        final Map<String, Object> messages = loadMessages();

        this.dataSource = DataSourceFactory.create(config.database());
        try {
            SchemaInitializer.initialize(dataSource, config.database().tablePrefix());
        } catch (SQLException exception) {
            getLogger().severe("Failed to initialise the database schema: " + exception.getMessage());
        }
        this.repositoryExecutor = new RepositoryExecutor();
        final DatabaseAvailability availability = new DatabaseAvailability(logSink);
        final ReportRepository reportRepository =
                new ReportRepository(dataSource, config.database().tablePrefix(), repositoryExecutor, availability);
        final StaffSettingsRepository staffSettingsRepository =
                new StaffSettingsRepository(dataSource, config.database().tablePrefix(), repositoryExecutor, availability);

        final MainThread mainThread = new MainThread(this);
        final MessageService messageService = new MessageService(messages, logSink);
        final FrameSender frameSender = new FrameSender(this);
        final ValidReportCache cache = new ValidReportCache();
        final PendingResolveRegistry pendingResolveRegistry = new PendingResolveRegistry();
        final PendingTeleportRegistry pendingTeleportRegistry = new PendingTeleportRegistry();
        final ReturnPositionRegistry returnPositionRegistry = new ReturnPositionRegistry();
        final ReconciliationService reconciliationService =
                new ReconciliationService(reportRepository, cache, mainThread, logSink);
        final StaffSettingsCache staffSettingsCache =
                new StaffSettingsCache(staffSettingsRepository, config.behaviour().defaultNotificationsEnabled(), mainThread);
        final NotificationService notificationService =
                new NotificationService(config.permissions(), staffSettingsCache, messageService);
        final TeleportFlowService teleportFlowService = new TeleportFlowService(
                reportRepository, frameSender, messageService, mainThread, returnPositionRegistry, config);
        final ReportSubmissionService submissionService = new ReportSubmissionService(
                config, reportRepository, messageService, frameSender, pendingResolveRegistry, mainThread, cache, notificationService);
        final ReportMenuService menuService = new ReportMenuService(
                menuConfig, config, messageService, reportRepository, frameSender, pendingResolveRegistry, mainThread);

        getServer().getMessenger().registerOutgoingPluginChannel(this, ProtocolChannel.IDENTIFIER);
        getServer().getMessenger().registerIncomingPluginChannel(this, ProtocolChannel.IDENTIFIER, new FrameDispatcher(
                getLogger(),
                frameSender,
                pendingResolveRegistry,
                cache,
                notificationService,
                teleportFlowService,
                pendingTeleportRegistry,
                reconciliationService,
                config.permissions().exempt()));

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(reconciliationService, staffSettingsCache, pendingTeleportRegistry, messageService), this);
        getServer().getPluginManager().registerEvents(new ReportMenuListener(menuService, teleportFlowService), this);

        ReportCommand.register(this, submissionService);
        ReportsCommand.register(this, menuService);
        ToggleReportCommand.register(this, config.permissions(), staffSettingsCache, messageService, mainThread);
        SReportsTpCommand.register(this, config.permissions(), teleportFlowService, messageService);

        reconciliationService.reconcile();
        final long intervalTicks = ticksFor(config.behaviour().reconciliationInterval());
        getServer().getScheduler().runTaskTimer(this, reconciliationService::reconcile, intervalTicks, intervalTicks);

        final long menuRefreshTicks = ticksFor(config.behaviour().menuRefreshInterval());
        getServer().getScheduler().runTaskTimer(
                this, () -> menuService.refreshOpenMenus(Instant.now()), menuRefreshTicks, menuRefreshTicks);
    }

    @Override
    public void onDisable() {
        if (repositoryExecutor != null) {
            repositoryExecutor.close();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private long ticksFor(Duration duration) {
        return Math.max(1L, duration.toMillis() / 50L);
    }

    private ConfigLoadOutcome<PaperConfig> loadConfig() {
        final Path configFile = getDataFolder().toPath().resolve("config.yml");
        final YamlLoadResult result = YamlDocumentLoader.load(configFile, "config.yml", getClass().getClassLoader());
        if (result instanceof YamlLoadResult.ParseFailure failure) {
            return new ConfigLoadOutcome.Failed<>(failure.fileName(), failure.reason());
        }
        final Map<String, Object> root = ((YamlLoadResult.Loaded) result).root();
        return new ConfigLoadOutcome.Ready<>(PaperConfig.fromRoot(new ConfigAccessor(root, logSink)));
    }

    private MenuConfig loadMenuConfig() {
        final Path menuFile = getDataFolder().toPath().resolve("menu.yml");
        final YamlLoadResult result = YamlDocumentLoader.load(menuFile, "menu.yml", getClass().getClassLoader());
        if (result instanceof YamlLoadResult.Loaded loaded) {
            return MenuConfigLoader.load(new ConfigAccessor(loaded.root(), logSink), logSink);
        }
        if (result instanceof YamlLoadResult.ParseFailure failure) {
            getLogger().severe("Failed to parse " + failure.fileName() + ": " + failure.reason() + "; using the default menu layout");
        }
        return MenuConfig.defaultLayout();
    }

    private Map<String, Object> loadMessages() {
        final Path messagesFile = getDataFolder().toPath().resolve("messages.yml");
        final YamlLoadResult result = YamlDocumentLoader.load(messagesFile, "messages.yml", getClass().getClassLoader());
        if (result instanceof YamlLoadResult.Loaded loaded) {
            return loaded.root();
        }
        if (result instanceof YamlLoadResult.ParseFailure failure) {
            getLogger().severe("Failed to parse " + failure.fileName() + ": " + failure.reason());
        }
        return Map.of();
    }
}
