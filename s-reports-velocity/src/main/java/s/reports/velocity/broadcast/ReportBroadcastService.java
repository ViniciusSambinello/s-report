package s.reports.velocity.broadcast;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import s.reports.common.protocol.FrameCodec;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.common.protocol.ReportFrame;

public final class ReportBroadcastService {

    private final ProxyServer proxyServer;
    private final Logger logger;

    public ReportBroadcastService(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    public void broadcastCreated(ServerConnection source, ReportFrame.ReportCreated frame) {
        broadcast(source, frame);
    }

    public void broadcastDismissed(ServerConnection source, ReportFrame.ReportDismissed frame) {
        broadcast(source, frame);
    }

    private void broadcast(ServerConnection source, ReportFrame frame) {
        final byte[] payload = FrameCodec.encode(frame);
        final String originName = source.getServerInfo().getName();
        for (final RegisteredServer server : proxyServer.getAllServers()) {
            if (server.getServerInfo().getName().equals(originName)) {
                continue;
            }
            if (server.getPlayersConnected().isEmpty()) {
                logger.warn("Skipping unreachable server {} (no online players) for a report broadcast",
                        server.getServerInfo().getName());
                continue;
            }
            server.sendPluginMessage(MinecraftChannelIdentifier.from(ProtocolChannel.IDENTIFIER), payload);
        }
    }
}
