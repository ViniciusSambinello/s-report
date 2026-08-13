package s.reports.paper.message;

import java.util.Map;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import s.reports.common.logging.LogSink;

public final class MessageService {

    private final Map<String, Object> messages;
    private final MiniMessage miniMessage;
    private final LogSink logSink;

    public MessageService(Map<String, Object> messages, LogSink logSink) {
        this.messages = messages;
        this.miniMessage = MiniMessage.miniMessage();
        this.logSink = logSink;
    }

    public void send(Audience audience, String key) {
        send(audience, key, Map.of());
    }

    public void send(Audience audience, String key, Map<String, String> placeholders) {
        final String rendered = renderRaw(key, placeholders);
        if (rendered == null || rendered.isEmpty()) {
            return;
        }
        audience.sendMessage(miniMessage.deserialize(rendered));
    }

    public Component render(String key, Map<String, String> placeholders) {
        final String rendered = renderRaw(key, placeholders);
        return miniMessage.deserialize(rendered == null ? "" : rendered);
    }

    private String renderRaw(String key, Map<String, String> placeholders) {
        final Object raw = messages.get(key);
        if (raw == null) {
            logSink.warn("Missing message key '" + key + "'");
            return null;
        }
        String rendered = String.valueOf(raw);
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return rendered;
    }
}
