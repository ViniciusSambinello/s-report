package s.reports.common.config;

@FunctionalInterface
public interface ConfigLogSink {

    void warn(String message);
}
