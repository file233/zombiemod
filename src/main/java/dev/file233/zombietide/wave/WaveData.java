package dev.file233.zombietide.wave;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent wave-cycle state, stored on the overworld's data storage so the
 * apocalypse remembers itself across restarts.
 */
public final class WaveData extends SavedData {
    public static final String DATA_NAME = "zombietide_waves";

    public static final int PHASE_IDLE = 0;
    public static final int PHASE_ACTIVE = 1;

    /** Wave that is running or was started last (0 = nothing happened yet). */
    public int wave = 0;
    /** PHASE_IDLE (counting down to the next wave) or PHASE_ACTIVE. */
    public int phase = PHASE_IDLE;
    /** Ticks left in the current phase. */
    public long ticksRemaining;
    /** Total length of the current phase (for the HUD progress bar). */
    public long phaseTotal;
    /** Number of waves fully survived (never decreases, even in LOOP mode). */
    public int completed = 0;
    /** Cycle frozen by /zombietide pause. */
    public boolean paused = false;
    /** True when waves.afterLastWave=STOP and the final wave was survived. */
    public boolean finished = false;

    public WaveData() {}

    public static SavedData.Factory<WaveData> factory() {
        return new SavedData.Factory<>(WaveData::new, WaveData::load, null);
    }

    public static WaveData load(CompoundTag tag, HolderLookup.Provider registries) {
        WaveData d = new WaveData();
        d.wave = tag.getInt("wave");
        d.phase = tag.getInt("phase");
        d.ticksRemaining = tag.getLong("ticksRemaining");
        d.phaseTotal = Math.max(1L, tag.getLong("phaseTotal"));
        d.completed = tag.getInt("completed");
        d.paused = tag.getBoolean("paused");
        d.finished = tag.getBoolean("finished");
        // Graceful sanity: never boot mid-"alarm window" with a huge stale countdown.
        if (d.ticksRemaining < 0 || d.ticksRemaining > d.phaseTotal) d.ticksRemaining = d.phaseTotal;
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("wave", wave);
        tag.putInt("phase", phase);
        tag.putLong("ticksRemaining", ticksRemaining);
        tag.putLong("phaseTotal", phaseTotal);
        tag.putInt("completed", completed);
        tag.putBoolean("paused", paused);
        tag.putBoolean("finished", finished);
        return tag;
    }
}
