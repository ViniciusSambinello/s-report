package s.reports.common.protocol;

public sealed interface FrameDecodeResult permits FrameDecodeResult.Accepted, FrameDecodeResult.Rejected {

    record Accepted(ReportFrame frame) implements FrameDecodeResult {
    }

    record Rejected(String reason) implements FrameDecodeResult {
    }
}
