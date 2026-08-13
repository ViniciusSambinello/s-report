package s.reports.common.logging;

public interface LogSink {

    void warn(String message);

    void severe(String message);
}
