package dev.file233.zombietide.network;

import dev.file233.zombietide.ZombieTide;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * One-per-second server → client snapshot driving the tiny HUD.
 *
 * <p>{@code phase}: 0 = counting down to the next wave, 1 = wave running.
 * {@code flags}: bit0 = paused, bit1 = finished (survived the last wave), bit2 = alarm sounding.
 */
public record WaveSyncPayload(
        int wave,
        int maxWaves,
        byte phase,
        long ticksRemaining,
        long phaseTotal,
        int zombiesAlive,
        byte flags) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WaveSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ZombieTide.MOD_ID, "wave_sync"));

    public static final StreamCodec<FriendlyByteBuf, WaveSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public WaveSyncPayload decode(FriendlyByteBuf buf) {
            int wave = buf.readVarInt();
            int maxWaves = buf.readVarInt();
            byte phase = buf.readByte();
            long remaining = buf.readVarLong();
            long total = buf.readVarLong();
            int zombies = buf.readVarInt();
            byte flags = buf.readByte();
            return new WaveSyncPayload(wave, maxWaves, phase, remaining, total, zombies, flags);
        }

        @Override
        public void encode(FriendlyByteBuf buf, WaveSyncPayload payload) {
            buf.writeVarInt(payload.wave);
            buf.writeVarInt(payload.maxWaves);
            buf.writeByte(payload.phase);
            buf.writeVarLong(payload.ticksRemaining);
            buf.writeVarLong(payload.phaseTotal);
            buf.writeVarInt(payload.zombiesAlive);
            buf.writeByte(payload.flags);
        }
    };

    @Override
    public CustomPacketPayload.Type<WaveSyncPayload> type() {
        return TYPE;
    }

    public boolean paused() { return (flags & 1) != 0; }
    public boolean finished() { return (flags & 2) != 0; }
    public boolean alarmSounding() { return (flags & 4) != 0; }
    public boolean waveActive() { return phase == 1; }
}
