package s.reports.common.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import s.reports.common.domain.TeleportDenyReason;

class FrameCodecTest {

    @Test
    void roundTripsTargetResolveRequest() {
        assertRoundTrip(new ReportFrame.TargetResolveRequest(UUID.randomUUID(), UUID.randomUUID(), "Steve"));
    }

    @Test
    void roundTripsTargetProbe() {
        assertRoundTrip(new ReportFrame.TargetProbe(UUID.randomUUID(), "survival-1", UUID.randomUUID()));
    }

    @Test
    void roundTripsTargetProbeResult() {
        assertRoundTrip(new ReportFrame.TargetProbeResult(UUID.randomUUID(), "survival-1", UUID.randomUUID(), "Steve", true));
    }

    @Test
    void roundTripsTargetResolveResponse() {
        assertRoundTrip(new ReportFrame.TargetResolveResponse(
                UUID.randomUUID(), true, UUID.randomUUID(), "Steve", "survival-2", false));
    }

    @Test
    void roundTripsReportCreated() {
        assertRoundTrip(new ReportFrame.ReportCreated(
                UUID.randomUUID(), UUID.randomUUID(), "Steve", UUID.randomUUID(), "Alex",
                "using kill aura", "survival-1", 1000L, 4600L, 7L));
    }

    @Test
    void roundTripsReportDismissed() {
        assertRoundTrip(new ReportFrame.ReportDismissed(UUID.randomUUID(), UUID.randomUUID(), 12345L));
    }

    @Test
    void roundTripsTeleportRequest() {
        assertRoundTrip(new ReportFrame.TeleportRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10000L));
    }

    @Test
    void roundTripsTeleportArm() {
        assertRoundTrip(new ReportFrame.TeleportArm(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 999999L));
    }

    @Test
    void roundTripsTeleportGrant() {
        assertRoundTrip(new ReportFrame.TeleportGrant(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void roundTripsTeleportDeniedForEveryReason() {
        for (final TeleportDenyReason reason : TeleportDenyReason.values()) {
            assertRoundTrip(new ReportFrame.TeleportDenied(UUID.randomUUID(), UUID.randomUUID(), reason));
        }
    }

    @Test
    void roundTripsSyncRequest() {
        assertRoundTrip(new ReportFrame.SyncRequest("survival-1"));
    }

    @Test
    void rejectsUnsupportedProtocolVersion() {
        final byte[] encoded = FrameCodec.encode(new ReportFrame.SyncRequest("survival-1"));
        encoded[0] = (byte) 99;
        assertInstanceOf(FrameDecodeResult.Rejected.class, FrameCodec.decode(encoded));
    }

    @Test
    void rejectsUnrecognisedFrameType() {
        final byte[] encoded = FrameCodec.encode(new ReportFrame.SyncRequest("survival-1"));
        encoded[1] = (byte) 99;
        assertInstanceOf(FrameDecodeResult.Rejected.class, FrameCodec.decode(encoded));
    }

    @Test
    void rejectsTruncatedPayload() {
        final byte[] encoded = FrameCodec.encode(
                new ReportFrame.TeleportRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1000L));
        final byte[] truncated = new byte[encoded.length - 5];
        System.arraycopy(encoded, 0, truncated, 0, truncated.length);
        assertInstanceOf(FrameDecodeResult.Rejected.class, FrameCodec.decode(truncated));
    }

    @Test
    void rejectsEmptyPayload() {
        assertInstanceOf(FrameDecodeResult.Rejected.class, FrameCodec.decode(new byte[0]));
    }

    private void assertRoundTrip(ReportFrame frame) {
        final byte[] encoded = FrameCodec.encode(frame);
        final FrameDecodeResult.Accepted accepted = assertInstanceOf(FrameDecodeResult.Accepted.class, FrameCodec.decode(encoded));
        assertEquals(frame, accepted.frame());
    }
}
