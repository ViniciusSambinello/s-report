package s.reports.velocity.sync;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import s.reports.common.protocol.FrameCodec;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.common.protocol.ReportFrame;

public final class SyncEchoService {

    public void handle(ServerConnection source, ReportFrame.SyncRequest request) {
        source.getServer().sendPluginMessage(
                MinecraftChannelIdentifier.from(ProtocolChannel.IDENTIFIER),
                FrameCodec.encode(new ReportFrame.SyncRequest(request.serverName())));
    }
}
