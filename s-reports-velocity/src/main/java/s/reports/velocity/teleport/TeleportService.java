package s.reports.velocity.teleport;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import s.reports.common.domain.TeleportDenyReason;
import s.reports.common.protocol.FrameCodec;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.common.protocol.ReportFrame;

public final class TeleportService {

    private final ProxyServer proxyServer;
    private final Logger logger;

    public TeleportService(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    public void handleTeleportRequest(ServerConnection origin, ReportFrame.TeleportRequest request) {
        final Optional<Player> target = proxyServer.getPlayer(request.targetId());
        final Optional<ServerConnection> targetConnection = target.flatMap(Player::getCurrentServer);
        if (target.isEmpty() || targetConnection.isEmpty()) {
            deny(origin, request, TeleportDenyReason.TARGET_OFFLINE);
            return;
        }
        final Optional<Player> staff = proxyServer.getPlayer(request.staffId());
        if (staff.isEmpty()) {
            logger.warn("Ignoring TeleportRequest for a staff member who is no longer online");
            return;
        }
        final RegisteredServer targetServer = targetConnection.get().getServer();
        final String staffServerName = staff.get().getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("");
        if (targetServer.getServerInfo().getName().equals(staffServerName)) {
            send(origin, new ReportFrame.TeleportGrant(request.staffId(), request.targetId(), request.reportId()));
            return;
        }
        armAndTransfer(origin, request, staff.get(), targetServer);
    }

    private void armAndTransfer(
            ServerConnection origin, ReportFrame.TeleportRequest request, Player staff, RegisteredServer targetServer) {
        final long expiresAt = Instant.now().toEpochMilli() + request.pendingTeleportTimeoutMillis();
        send(targetServer, new ReportFrame.TeleportArm(request.staffId(), request.targetId(), request.reportId(), expiresAt));
        staff.createConnectionRequest(targetServer).connect().whenComplete((result, throwable) -> {
            if (throwable != null || !result.isSuccessful()) {
                deny(origin, request, TeleportDenyReason.TRANSFER_FAILED);
            }
        });
    }

    private void deny(ServerConnection origin, ReportFrame.TeleportRequest request, TeleportDenyReason reason) {
        send(origin, new ReportFrame.TeleportDenied(request.staffId(), request.reportId(), reason));
    }

    private void send(ServerConnection destination, ReportFrame frame) {
        send(destination.getServer(), frame);
    }

    private void send(RegisteredServer destination, ReportFrame frame) {
        destination.sendPluginMessage(MinecraftChannelIdentifier.from(ProtocolChannel.IDENTIFIER), FrameCodec.encode(frame));
    }
}
