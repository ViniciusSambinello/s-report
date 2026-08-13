package s.reports.paper.logging;

import java.util.logging.Logger;
import s.reports.common.logging.LogSink;

public final class PaperLogSink implements LogSink {

    private final Logger logger;

    public PaperLogSink(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
    }

    @Override
    public void severe(String message) {
        logger.severe(message);
    }
}
