package s.reports.common.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import s.reports.common.domain.TeleportDenyReason;

public final class FrameCodec {

    public static final int PROTOCOL_VERSION = 1;

    private FrameCodec() {
    }

    public static byte[] encode(ReportFrame frame) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeByte(PROTOCOL_VERSION);
            out.writeByte(FrameType.of(frame).id());
            writePayload(out, frame);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return buffer.toByteArray();
    }

    public static FrameDecodeResult decode(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            final int version = in.readUnsignedByte();
            if (version != PROTOCOL_VERSION) {
                return new FrameDecodeResult.Rejected("unsupported protocol version " + version);
            }
            final int typeId = in.readUnsignedByte();
            final var type = FrameType.fromId(typeId);
            if (type.isEmpty()) {
                return new FrameDecodeResult.Rejected("unrecognised frame type " + typeId);
            }
            return new FrameDecodeResult.Accepted(readPayload(in, type.get()));
        } catch (EOFException exception) {
            return new FrameDecodeResult.Rejected("truncated payload");
        } catch (IOException | RuntimeException exception) {
            return new FrameDecodeResult.Rejected("malformed payload: " + exception.getMessage());
        }
    }

    private static void writePayload(DataOutputStream out, ReportFrame frame) throws IOException {
        switch (frame) {
            case ReportFrame.TargetResolveRequest f -> {
                writeUuid(out, f.requestId());
                writeUuid(out, f.reporterId());
                out.writeUTF(f.targetName());
            }
            case ReportFrame.TargetProbe f -> {
                writeUuid(out, f.requestId());
                out.writeUTF(f.originServer());
                writeUuid(out, f.targetId());
            }
            case ReportFrame.TargetProbeResult f -> {
                writeUuid(out, f.requestId());
                out.writeUTF(f.originServer());
                writeUuid(out, f.targetId());
                out.writeUTF(f.targetName());
                out.writeBoolean(f.exempt());
            }
            case ReportFrame.TargetResolveResponse f -> {
                writeUuid(out, f.requestId());
                out.writeBoolean(f.found());
                writeUuid(out, f.targetId());
                out.writeUTF(f.targetName());
                out.writeUTF(f.currentServer());
                out.writeBoolean(f.exempt());
            }
            case ReportFrame.ReportCreated f -> {
                writeUuid(out, f.reportId());
                writeUuid(out, f.targetId());
                out.writeUTF(f.targetName());
                writeUuid(out, f.reporterId());
                out.writeUTF(f.reporterName());
                out.writeUTF(f.reason());
                out.writeUTF(f.originServer());
                out.writeLong(f.createdAtEpochMilli());
                out.writeLong(f.expiresAtEpochMilli());
            }
            case ReportFrame.ReportDismissed f -> {
                writeUuid(out, f.reportId());
                writeUuid(out, f.dismissedBy());
                out.writeLong(f.dismissedAtEpochMilli());
            }
            case ReportFrame.TeleportRequest f -> {
                writeUuid(out, f.staffId());
                writeUuid(out, f.reportId());
                writeUuid(out, f.targetId());
            }
            case ReportFrame.TeleportArm f -> {
                writeUuid(out, f.staffId());
                writeUuid(out, f.targetId());
                writeUuid(out, f.reportId());
                out.writeLong(f.expiresAtEpochMilli());
            }
            case ReportFrame.TeleportGrant f -> {
                writeUuid(out, f.staffId());
                writeUuid(out, f.targetId());
                writeUuid(out, f.reportId());
            }
            case ReportFrame.TeleportDenied f -> {
                writeUuid(out, f.staffId());
                writeUuid(out, f.reportId());
                out.writeByte(f.reason().ordinal());
            }
            case ReportFrame.SyncRequest f -> out.writeUTF(f.serverName());
        }
    }

    private static ReportFrame readPayload(DataInputStream in, FrameType type) throws IOException {
        return switch (type) {
            case TARGET_RESOLVE_REQUEST -> new ReportFrame.TargetResolveRequest(readUuid(in), readUuid(in), in.readUTF());
            case TARGET_PROBE -> new ReportFrame.TargetProbe(readUuid(in), in.readUTF(), readUuid(in));
            case TARGET_PROBE_RESULT -> new ReportFrame.TargetProbeResult(
                    readUuid(in), in.readUTF(), readUuid(in), in.readUTF(), in.readBoolean());
            case TARGET_RESOLVE_RESPONSE -> new ReportFrame.TargetResolveResponse(
                    readUuid(in), in.readBoolean(), readUuid(in), in.readUTF(), in.readUTF(), in.readBoolean());
            case REPORT_CREATED -> new ReportFrame.ReportCreated(
                    readUuid(in),
                    readUuid(in),
                    in.readUTF(),
                    readUuid(in),
                    in.readUTF(),
                    in.readUTF(),
                    in.readUTF(),
                    in.readLong(),
                    in.readLong());
            case REPORT_DISMISSED -> new ReportFrame.ReportDismissed(readUuid(in), readUuid(in), in.readLong());
            case TELEPORT_REQUEST -> new ReportFrame.TeleportRequest(readUuid(in), readUuid(in), readUuid(in));
            case TELEPORT_ARM -> new ReportFrame.TeleportArm(readUuid(in), readUuid(in), readUuid(in), in.readLong());
            case TELEPORT_GRANT -> new ReportFrame.TeleportGrant(readUuid(in), readUuid(in), readUuid(in));
            case TELEPORT_DENIED -> new ReportFrame.TeleportDenied(readUuid(in), readUuid(in), readDenyReason(in));
            case SYNC_REQUEST -> new ReportFrame.SyncRequest(in.readUTF());
        };
    }

    private static void writeUuid(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        final long mostSignificantBits = in.readLong();
        final long leastSignificantBits = in.readLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    private static TeleportDenyReason readDenyReason(DataInputStream in) throws IOException {
        final int ordinal = in.readUnsignedByte();
        final TeleportDenyReason[] values = TeleportDenyReason.values();
        if (ordinal >= values.length) {
            throw new IOException("unrecognised teleport deny reason ordinal " + ordinal);
        }
        return values[ordinal];
    }
}
