package s.reports.velocity.logging;

import org.slf4j.Logger;
import s.reports.common.logging.LogSink;

public final class VelocityLogSink implements LogSink {

    private final Logger logger;

    public VelocityLogSink(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void severe(String message) {
        logger.error(message);
    }
}
