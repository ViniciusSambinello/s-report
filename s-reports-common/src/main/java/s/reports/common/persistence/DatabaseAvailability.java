package s.reports.common.persistence;

import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import s.reports.common.logging.LogSink;

public final class DatabaseAvailability {

    private final AtomicBoolean available = new AtomicBoolean(true);
    private final LogSink logSink;

    public DatabaseAvailability(LogSink logSink) {
        this.logSink = Objects.requireNonNull(logSink, "logSink");
    }

    void reportFailure(SQLException exception) {
        if (available.compareAndSet(true, false)) {
            logSink.severe("Lost connection to the database: " + exception.getMessage());
        }
    }

    void reportSuccess() {
        if (available.compareAndSet(false, true)) {
            logSink.warn("Connection to the database recovered");
        }
    }

    public boolean isAvailable() {
        return available.get();
    }
}
