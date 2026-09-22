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
            // Placeholder values (e.g. a report's reason, or a player-chosen name) are
            // untrusted free text. Escape any MiniMessage tag syntax they contain before
            // splicing them into the template so they render as literal text instead of
            // being parsed as components (which could otherwise forge <click>/<hover>
            // elements shown to staff).
            rendered = rendered.replace("%" + entry.getKey() + "%", miniMessage.escapeTags(entry.getValue()));
        }
        return rendered;
    }
}
