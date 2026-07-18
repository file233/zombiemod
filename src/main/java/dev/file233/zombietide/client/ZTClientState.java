package dev.file233.zombietide.client;

import dev.file233.zombietide.network.WaveSyncPayload;

/** Client-side mirror of the server wave state + the red-fog trauma accumulator. */
public final class ZTClientState {
    private ZTClientState() {}

    // ---- wave sync cache ----
    public static boolean hasData = false;
    public static int wave = 0;
    public static int maxWaves = 50;
    public static byte phase = 0;
    public static long ticksRemaining = 0L;
    public static long phaseTotal = 1L;
    public static int zombiesAlive = 0;
    public static boolean paused = false;
    public static boolean finished = false;
    public static boolean alarmSounding = false;

    public static boolean waveActive() { return phase == 1; }

    // ---- trauma (red fog) ----
    public static float trauma = 0.0F;
    private static float lastHealth = -1.0F;

    public static void apply(WaveSyncPayload payload) {
        hasData = true;
        wave = payload.wave();
        maxWaves = payload.maxWaves();
        phase = payload.phase();
        ticksRemaining = payload.ticksRemaining();
        phaseTotal = Math.max(1L, payload.phaseTotal());
        zombiesAlive = payload.zombiesAlive();
        paused = payload.paused();
        finished = payload.finished();
        alarmSounding = payload.alarmSounding();
    }

    public static void reset() {
        hasData = false;
        trauma = 0.0F;
        lastHealth = -1.0F;
    }

    /** Approximate countdown between server sync packets (1s cadence from the server). */
    public static long smoothTicksRemaining(float partialTicks) {
        return ticksRemaining;
    }

    /** Track the local player's health and charge the red fog on damage. */
    public static void tickDamageWatch(net.minecraft.client.player.LocalPlayer player) {
        float health = player.getHealth();
        if (lastHealth < 0.0F) {
            lastHealth = health;
        } else if (health < lastHealth) {
            float lost = lastHealth - health;
            float max = Math.max(1.0F, player.getMaxHealth());
            trauma = Math.min(1.0F, trauma + (lost / max) * 1.8F * (float) ZTClientConfig.OVERLAY_INTENSITY.get().doubleValue());
            lastHealth = health;
        } else {
            lastHealth = health;
        }
        // slow recovery from the shock
        if (trauma > 0.0F) {
            trauma = Math.max(0.0F, trauma - 0.006F);
        }
    }
}
