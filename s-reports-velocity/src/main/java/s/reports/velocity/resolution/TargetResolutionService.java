package s.reports.velocity.resolution;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import s.reports.common.protocol.FrameCodec;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.common.protocol.ReportFrame;

public final class TargetResolutionService {

    private static final UUID NO_TARGET = new UUID(0, 0);

    private final ProxyServer proxyServer;
    private final Logger logger;

    public TargetResolutionService(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    public void handleResolveRequest(ServerConnection origin, ReportFrame.TargetResolveRequest request) {
        final Optional<Player> target = proxyServer.getPlayer(request.targetName());
        final Optional<ServerConnection> targetServer = target.flatMap(Player::getCurrentServer);
        if (target.isEmpty() || targetServer.isEmpty()) {
            send(origin.getServer(), new ReportFrame.TargetResolveResponse(request.requestId(), false, NO_TARGET, "", "", false));
            return;
        }
        final Player player = target.get();
        send(targetServer.get().getServer(), new ReportFrame.TargetProbe(
                request.requestId(), origin.getServerInfo().getName(), player.getUniqueId()));
    }

    public void handleProbeResult(ReportFrame.TargetProbeResult result) {
        final Optional<RegisteredServer> originServer = proxyServer.getServer(result.originServer());
        if (originServer.isEmpty()) {
            logger.warn("Cannot route TargetResolveResponse: origin server {} is no longer registered", result.originServer());
            return;
        }
        final Optional<Player> target = proxyServer.getPlayer(result.targetId());
        final String currentServer = target.flatMap(Player::getCurrentServer)
                .map(connection -> connection.getServerInfo().getName())
                .orElse("");
        send(originServer.get(), new ReportFrame.TargetResolveResponse(
                result.requestId(), !currentServer.isEmpty(), result.targetId(), result.targetName(), currentServer, result.exempt()));
    }

    private void send(RegisteredServer destination, ReportFrame frame) {
        destination.sendPluginMessage(MinecraftChannelIdentifier.from(ProtocolChannel.IDENTIFIER), FrameCodec.encode(frame));
    }
}
