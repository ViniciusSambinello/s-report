package s.reports.paper.messaging;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import s.reports.common.protocol.FrameCodec;
import s.reports.common.protocol.ProtocolChannel;
import s.reports.common.protocol.ReportFrame;

public final class FrameSender {

    private final Plugin plugin;

    public FrameSender(Plugin plugin) {
        this.plugin = plugin;
    }

    public void send(Player carrier, ReportFrame frame) {
        carrier.sendPluginMessage(plugin, ProtocolChannel.IDENTIFIER, FrameCodec.encode(frame));
    }
}
