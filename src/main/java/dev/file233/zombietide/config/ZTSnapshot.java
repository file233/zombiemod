package dev.file233.zombietide.config;

/**
 * Performance snapshot of {@link ZTConfig}: an immutable, allocation-free view of every
 * hot config value. Every per-wave growth curve is kept in <b>closed form</b> — each
 * formula is linear in the design wave, so a handful of precomputed slopes reproduces the
 * exact values a lookup table would, using literally <b>~0 bytes of heap</b> (no per-wave
 * arrays, even at {@code maxWaves = 100000}).
 *
 * <p><b>Why it exists:</b> the raw config getters walk NeoForge's spec map on every call.
 * That is negligible once per second — but the spawn engine, the AI goals of hundreds of
 * zombies and the HUD ask for dozens of values <i>every tick</i>. This snapshot turns each
 * of those reads into a plain field access plus a multiply-add, so the server spends its
 * budget simulating the apocalypse instead of looking up maps.
 *
 * <p>Refreshed exactly when configuration can change: on config load/reload and after
 * every command-driven edit. Callers never mutate it; the reference is published
 * {@code volatile} so every thread sees a coherent view.
 */
public final class ZTSnapshot {
    private static volatile ZTSnapshot CURRENT;

    /** Rebuilds the snapshot from the live config. Call after config load/reload/edit. */
    public static synchronized void refresh() {
        ZTSnapshot old = CURRENT;
        CURRENT = new ZTSnapshot(old == null ? 1 : old.epoch + 1);
    }

    /** Current snapshot. Built lazily so class init can never race NeoForge's spec loading. */
    public static ZTSnapshot get() {
        ZTSnapshot s = CURRENT;
        if (s == null) {
            refresh();
            s = CURRENT;
        }
        return s;
    }

    /** Identity token: bumps on every snapshot rebuild, letting other caches invalidate themselves. */
    public static int epoch() { return get().epoch; }

    // ------------------------------------------------------------------
    public final int epoch;

    // general
    public final boolean enabled;

    // waves
    public final int maxWaves;
    /** designWave slope: designWave(w) = w * norm — the single scaling constant of every curve. */
    public final double norm;
    public final boolean chatAnnounce, titleAnnounce;
    private final double firstWaveMinutes, waveIncrementMinutes;

    // the two dials
    public final double intelligenceMax, unseenMemoryBase;
    public final double frenzyIntensity, frenzySpeedBoost, frenzyHearingBonus, frenzyNoiseFactor;
    private final double intelligenceBase, intelligencePerWave;
    private final int unseenMemoryTicksBase, unseenMemoryPerWave;

    // zombies
    public final boolean stripArmor, blockItemsOnly, convertVariants, noBurnInWaves;
    public final boolean huntWithoutSight, blockBreakOnlyWaves;
    public final double heldBlockChance, blockBreakChance, blockBreakMaxHardness;
    public final double babyKeepChance, variantKeepChance, nonZombieKeepChance;
    public final double maxDamage, healthMaxAbovePlayer, reinforcementCap;
    public final int blockBreakCooldown, blockBreakFromWave, reinforcementFromWave;
    private final double baseSpeed, maxSpeed, speedPerWave;
    private final double followBase, followPerWave, followCap, followWaveBonus;
    private final double healthBase, healthPerWave;
    private final double kbrPerWave, reinforcementPerWave;
    private final double hearingBase, hearingPerWave, hearingCap;
    private final double damagePerWave;
    private final int noiseBaseTicks;

    // targeting
    public final boolean targetCreative, targetSpectators;

    // spawning
    public final boolean spawnerEnabled, daylightSpawn, boostNaturalPlacement;
    public final boolean ignoreGamerule, pressureCreative;
    public final int spawnIntervalTicks, spawnAttempts, ringMinSq, ringMaxSq, ringMin, ringMax;
    public final double surfaceChance, daySpawnFactor;
    private final int capPerPlayer, capMax;
    private final double capPerWave;

    // effects
    public final double effectProcChance;
    public final boolean effectsOnlyDuringWaves;
    public final int effectAmplifier;

    private ZTSnapshot(int epoch) {
        this.epoch = epoch;
        // ---- scalars (exactly one config read each, then never again) ----
        this.enabled = ZTConfig.ENABLED.get();
        this.maxWaves = Math.max(1, ZTConfig.MAX_WAVES.get());
        this.norm = 50.0D / this.maxWaves;
        this.chatAnnounce = ZTConfig.CHAT_ANNOUNCE.get();
        this.titleAnnounce = ZTConfig.TITLE_ANNOUNCE.get();
        this.firstWaveMinutes = ZTConfig.FIRST_WAVE_MINUTES.get();
        this.waveIncrementMinutes = ZTConfig.WAVE_INCREMENT_MINUTES.get();

        this.intelligenceMax = ZTConfig.I_MAX.get();
        this.intelligenceBase = ZTConfig.I_BASE.get();
        this.intelligencePerWave = ZTConfig.I_PER_WAVE.get();
        this.unseenMemoryTicksBase = ZTConfig.I_UNSEEN_MEMORY.get();
        this.unseenMemoryBase = this.unseenMemoryTicksBase;
        this.unseenMemoryPerWave = ZTConfig.I_UNSEEN_PER_WAVE.get();

        this.frenzyIntensity = ZTConfig.F_INTENSITY.get();
        this.frenzySpeedBoost = ZTConfig.F_SPEED_BOOST.get();
        this.frenzyHearingBonus = ZTConfig.F_HEARING_BONUS.get();
        this.frenzyNoiseFactor = ZTConfig.F_NOISE_FACTOR.get();

        this.stripArmor = ZTConfig.Z_STRIP_ARMOR.get();
        this.blockItemsOnly = ZTConfig.Z_BLOCK_ITEMS_ONLY.get();
        this.convertVariants = ZTConfig.Z_CONVERT_VARIANTS.get();
        this.noBurnInWaves = ZTConfig.Z_NO_BURN_IN_WAVES.get();
        this.huntWithoutSight = ZTConfig.Z_HUNT_WITHOUT_SIGHT.get();
        this.blockBreakOnlyWaves = ZTConfig.Z_BLOCK_BREAK_ONLY_WAVES.get();
        this.heldBlockChance = ZTConfig.Z_HELD_BLOCK_CHANCE.get();
        this.blockBreakChance = ZTConfig.Z_BLOCK_BREAK_CHANCE.get();
        this.blockBreakMaxHardness = ZTConfig.Z_BLOCK_BREAK_MAX_HARDNESS.get();
        this.babyKeepChance = ZTConfig.Z_BABY_KEEP_CHANCE.get();
        this.variantKeepChance = ZTConfig.Z_VARIANT_KEEP_CHANCE.get();
        this.nonZombieKeepChance = ZTConfig.Z_NON_ZOMBIE_KEEP_CHANCE.get();
        this.maxDamage = ZTConfig.Z_MAX_DAMAGE_HEARTS.get() * 2.0D;
        this.healthMaxAbovePlayer = ZTConfig.Z_HEALTH_MAX_ABOVE_PLAYER.get();
        this.reinforcementCap = ZTConfig.Z_REINFORCEMENT_CAP.get();
        this.blockBreakCooldown = ZTConfig.Z_BLOCK_BREAK_COOLDOWN.get();
        this.blockBreakFromWave = ZTConfig.Z_BLOCK_BREAK_FROM_WAVE.get();
        this.reinforcementFromWave = ZTConfig.Z_REINFORCEMENT_FROM_WAVE.get();

        this.baseSpeed = ZTConfig.Z_BASE_SPEED.get();
        this.maxSpeed = ZTConfig.Z_MAX_SPEED.get();
        this.speedPerWave = ZTConfig.Z_SPEED_PER_WAVE.get();
        this.followBase = ZTConfig.Z_FOLLOW_RANGE.get();
        this.followPerWave = ZTConfig.Z_FOLLOW_PER_WAVE.get();
        this.followCap = ZTConfig.Z_FOLLOW_CAP.get();
        this.followWaveBonus = ZTConfig.Z_FOLLOW_WAVE_BONUS.get();
        this.healthBase = ZTConfig.Z_HEALTH_BASE_HEARTS.get();
        this.healthPerWave = ZTConfig.Z_HEALTH_PER_WAVE_HEARTS.get();
        this.kbrPerWave = ZTConfig.Z_KBR_PER_WAVE.get();
        this.reinforcementPerWave = ZTConfig.Z_REINFORCEMENT_PER_WAVE.get();
        this.hearingBase = ZTConfig.Z_HEARING_RADIUS.get();
        this.hearingPerWave = ZTConfig.Z_HEARING_PER_WAVE.get();
        this.hearingCap = ZTConfig.Z_HEARING_CAP.get();
        this.damagePerWave = ZTConfig.Z_DAMAGE_PER_WAVE_HEARTS.get();
        this.noiseBaseTicks = ZTConfig.Z_NOISE_COOLDOWN_TICKS.get();

        this.targetCreative = ZTConfig.T_CREATIVE.get();
        this.targetSpectators = ZTConfig.T_SPECTATORS.get();

        this.spawnerEnabled = ZTConfig.S_ENABLED.get();
        this.daylightSpawn = ZTConfig.S_DAYLIGHT_SPAWN.get();
        this.boostNaturalPlacement = ZTConfig.S_BOOST_NATURAL_PLACEMENT.get();
        this.ignoreGamerule = ZTConfig.S_IGNORE_GAMERULE.get();
        this.pressureCreative = ZTConfig.S_PRESSURE_CREATIVE.get();
        this.spawnIntervalTicks = ZTConfig.S_INTERVAL_TICKS.get();
        this.spawnAttempts = ZTConfig.S_ATTEMPTS_PER_CYCLE.get();
        this.ringMin = ZTConfig.S_RING_MIN.get();
        this.ringMax = Math.max(this.ringMin + 1, ZTConfig.S_RING_MAX.get());
        this.ringMinSq = this.ringMin * this.ringMin;
        this.ringMaxSq = this.ringMax * this.ringMax;
        this.surfaceChance = ZTConfig.S_SURFACE_CHANCE.get();
        this.daySpawnFactor = ZTConfig.S_DAY_SPAWN_FACTOR.get();
        this.capPerPlayer = ZTConfig.S_CAP_PER_PLAYER.get();
        this.capPerWave = ZTConfig.S_CAP_PER_WAVE.get();
        this.capMax = ZTConfig.S_CAP_MAX.get();

        this.effectProcChance = ZTConfig.E_PROC_CHANCE.get();
        this.effectsOnlyDuringWaves = ZTConfig.E_ONLY_DURING_WAVES.get();
        this.effectAmplifier = ZTConfig.E_AMPLIFIER.get();
    }

    // ------------------------------------------------------------------ accessors
    private int clampWave(int wave) {
        return wave < 0 ? 0 : (wave > maxWaves ? maxWaves : wave);
    }

    /** Design wave: the wave number normalized onto the 50-wave design spine. */
    private double dw(int wave) {
        return clampWave(wave) * norm;
    }

    /** Intelligence level for a wave: base + growth, clamped to the cap. */
    public double intelligenceFor(int wave) {
        return Math.min(intelligenceMax, intelligenceBase + dw(wave) * intelligencePerWave);
    }

    /** Reach scaling from brains: intelligence×1 = 1.0×, ×3 = 1.5×, ×5 = 2.0×. */
    private double reachScale(int wave) {
        return 0.75D + 0.25D * intelligenceFor(wave);
    }

    /** Zombie max health for {@code wave}, capped at player + healthMaxAbovePlayer hearts. */
    public double healthFor(int wave, double playerMaxHealth) {
        double built = (healthBase + dw(wave) * healthPerWave) * 2.0D;
        double cap = Math.max(2.0D, playerMaxHealth) + healthMaxAbovePlayer * 2.0D;
        return Math.max(2.0D, Math.min(built, cap));
    }

    /** Zombie attack-damage attribute for {@code wave} (already hard-capped). */
    public double attackFor(int wave) {
        return Math.min(maxDamage, 3.0D + dw(wave) * damagePerWave * 2.0D);
    }

    /** Movement speed for {@code wave}. */
    public double speedFor(int wave, boolean active) {
        double base = baseSpeed + Math.max(0, dw(wave) - 1) * speedPerWave;
        return Math.min(maxSpeed, active ? base + frenzySpeedBoost * frenzyIntensity : base);
    }

    /** Follow range for {@code wave}. */
    public double followFor(int wave, boolean active) {
        double v = followBase + dw(wave) * followPerWave + (active ? followWaveBonus * frenzyIntensity : 0.0D);
        v *= reachScale(wave);
        return Math.min(followCap, v);
    }

    /** Hearing radius for {@code wave}. */
    public double hearingFor(int wave, boolean active) {
        double v = hearingBase + dw(wave) * hearingPerWave + (active ? frenzyHearingBonus * frenzyIntensity : 0.0D);
        v *= reachScale(wave);
        return Math.min(hearingCap, v);
    }

    /** Reinforcement-call chance for {@code wave} (vanilla 0.1 when the gate is shut). */
    public double reinforcementsFor(int wave, boolean active) {
        double dw = dw(wave);
        if (dw < reinforcementFromWave) return 0.1D;
        double v = 0.1D + dw * reinforcementPerWave;
        if (active) v *= 1.0D + 0.5D * frenzyIntensity;
        return Math.min(reinforcementCap, v);
    }

    /** Wave-only knockback resistance for {@code wave}. */
    public double kbrFor(int wave) {
        return Math.min(0.4D, dw(wave) * kbrPerWave * frenzyIntensity);
    }

    /** Alive-zombie cap per player for {@code wave}. */
    public double spawnCapFor(int wave) {
        return Math.min(capMax, capPerPlayer + clampWave(wave) * capPerWave);
    }

    /** Hunt-goal re-check cadence (ticks) for {@code wave}: smarter + frenzied = twitchier. */
    public int retargetTicksFor(int wave, boolean active) {
        double intel = intelligenceFor(wave);
        int interval = (int) Math.round(14.0D - 2.0D * intel - (active ? 3.0D * frenzyIntensity : 0.0D));
        return Math.max(2, Math.min(40, interval));
    }

    /** Lost-sight memory (ticks) for {@code wave}. */
    public int unseenTicksFor(int wave, boolean active) {
        int base = unseenMemoryTicksBase + (int) Math.round(dw(wave) * unseenMemoryPerWave);
        int ticks = active ? (int) Math.round(base * (1.0D + 0.5D * frenzyIntensity)) : base;
        return Math.max(10, Math.min(600, ticks));
    }

    /** Sense-engine cooldown (ticks) for {@code wave}: smarter reacts faster, frenzy faster still. */
    public int noiseTicksFor(int wave, boolean active) {
        double brainFactor = Math.max(0.5D, 1.0D - 0.08D * intelligenceFor(wave));
        double v = noiseBaseTicks * brainFactor * (active ? frenzyNoiseFactor : 1.0D);
        return Math.max(5, (int) Math.round(v));
    }

    /** Design-wave gate for the block-breaking behaviour. */
    public boolean breacherGateFor(int wave) {
        return dw(wave) >= blockBreakFromWave;
    }

    /** Calm gap (ticks) before wave {@code nextWave} arrives: its own override, else the default. */
    public long calmTicksFor(int nextWave) {
        Integer override = ZTConfig.intervalOverrides().get(clampWave(Math.max(1, nextWave)));
        long seconds = override != null ? override : ZTConfig.DEFAULT_CALM_SECONDS;
        return Math.max(100L, seconds * 20L);
    }

    /** Duration (ticks) of wave {@code wave}: override, else the 8min/+2min formula. */
    public long durationTicksFor(int wave) {
        int w = clampWave(Math.max(1, wave));
        Integer override = ZTConfig.durationOverrides().get(w);
        if (override != null) return Math.max(100L, override * 20L);
        return Math.max(100L, Math.round((firstWaveMinutes + (w - 1) * waveIncrementMinutes) * 1200.0D));
    }
}
