package s.reports.velocity.messaging;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import s.reports.common.protocol.FrameCodec;
import s.reports.common.protocol.FrameDecodeResult;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.common.protocol.ReportFrame;
import s.reports.velocity.broadcast.ReportBroadcastService;
import s.reports.velocity.resolution.TargetResolutionService;
import s.reports.velocity.sync.SyncEchoService;
import s.reports.velocity.teleport.TeleportService;

public final class FrameMessageListener {

    private final Logger logger;
    private final TargetResolutionService resolutionService;
    private final ReportBroadcastService broadcastService;
    private final TeleportService teleportService;
    private final SyncEchoService syncEchoService;

    public FrameMessageListener(
            Logger logger,
            TargetResolutionService resolutionService,
            ReportBroadcastService broadcastService,
            TeleportService teleportService,
            SyncEchoService syncEchoService) {
        this.logger = logger;
        this.resolutionService = resolutionService;
        this.broadcastService = broadcastService;
        this.teleportService = teleportService;
        this.syncEchoService = syncEchoService;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(MinecraftChannelIdentifier.from(ProtocolChannel.IDENTIFIER))) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection source)) {
            logger.warn("Ignoring an s-reports frame that did not originate from a backend server connection");
            return;
        }
        final FrameDecodeResult decoded = FrameCodec.decode(event.getData());
        if (decoded instanceof FrameDecodeResult.Rejected rejected) {
            logger.warn("Rejected a malformed s-reports frame from {}: {}", source.getServerInfo().getName(), rejected.reason());
            return;
        }
        dispatch(source, ((FrameDecodeResult.Accepted) decoded).frame());
    }

    private void dispatch(ServerConnection source, ReportFrame frame) {
        switch (frame) {
            case ReportFrame.TargetResolveRequest f -> resolutionService.handleResolveRequest(source, f);
            case ReportFrame.TargetProbeResult f -> resolutionService.handleProbeResult(f);
            case ReportFrame.ReportCreated f -> broadcastService.broadcastCreated(source, f);
            case ReportFrame.ReportDismissed f -> broadcastService.broadcastDismissed(source, f);
            case ReportFrame.TeleportRequest f -> teleportService.handleTeleportRequest(source, f);
            case ReportFrame.SyncRequest f -> syncEchoService.handle(source, f);
            case ReportFrame.TargetProbe f -> logUnexpected("TargetProbe", source);
            case ReportFrame.TargetResolveResponse f -> logUnexpected("TargetResolveResponse", source);
            case ReportFrame.TeleportArm f -> logUnexpected("TeleportArm", source);
            case ReportFrame.TeleportGrant f -> logUnexpected("TeleportGrant", source);
            case ReportFrame.TeleportDenied f -> logUnexpected("TeleportDenied", source);
        }
    }

    private void logUnexpected(String frameName, ServerConnection source) {
        logger.warn("Ignoring unexpected {} received from {}", frameName, source.getServerInfo().getName());
    }
}
