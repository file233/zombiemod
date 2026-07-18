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

    // ---- trauma (blood splatter) ----
    public static final int SPLATTER_VARIANTS = 3;
    public static float trauma = 0.0F;
    /** Which blood-splatter texture the latest hit threw at the screen. */
    public static int splatterIndex = 0;
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
            // a fresh splash of blood for every hit
            splatterIndex = ZTClientConfig.OVERLAY_SPLATTER_VARIANTS.get()
                    ? player.getRandom().nextInt(SPLATTER_VARIANTS)
                    : 0;
            lastHealth = health;
        } else {
            lastHealth = health;
        }
        // the blood slowly drains off your vision
        if (trauma > 0.0F) {
            float fade = (float) ZTClientConfig.OVERLAY_FADE_PER_TICK.get().doubleValue();
            trauma = Math.max(0.0F, trauma - fade);
        }
    }
}
