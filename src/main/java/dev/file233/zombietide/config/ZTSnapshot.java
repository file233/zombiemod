package dev.file233.zombietide.config;

import java.util.Map;

/**
 * Performance snapshot of {@link ZTConfig}: an immutable, allocation-free view of every
 * hot config value plus ALL per-wave math (attributes, reach, cadence, caps, gates)
 * precomputed into flat arrays.
 *
 * <p><b>Why it exists:</b> the raw config getters walk NeoForge's spec map on every call.
 * That is negligible once per second — but the spawn engine, the AI goals of hundreds of
 * zombies and the HUD ask for dozens of values <i>every tick</i>. This snapshot turns each
 * of those reads into a plain field access or a single array index, so the server spends
 * its budget simulating the apocalypse instead of looking up maps.
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

    /**
     * Cold (!) identity token: bumps on every snapshot rebuild, letting other lazy caches
     * (e.g. parsed client config lists) invalidate themselves without owning a listener.
     */
    public static int epoch() { return get().epoch; }

    // ------------------------------------------------------------------
    public final int epoch;

    // general
    public final boolean enabled;

    // waves
    public final int maxWaves;
    private final long[] calmTicksByWave;      // [maxWaves+1]
    private final long[] durationTicksByWave;  // [maxWaves+1]
    public final boolean chatAnnounce, titleAnnounce;

    // the two dials
    public final double intelligenceMax, unseenMemoryBase;
    public final double frenzyIntensity, frenzySpeedBoost, frenzyHearingBonus, frenzyNoiseFactor;

    // zombies
    public final boolean stripArmor, blockItemsOnly, convertVariants, noBurnInWaves;
    public final boolean huntWithoutSight, blockBreakOnlyWaves;
    public final double heldBlockChance, blockBreakChance, blockBreakMaxHardness;
    public final double babyKeepChance, variantKeepChance, nonZombieKeepChance;
    public final double maxDamage, healthMaxAbovePlayer, reinforcementCap;
    public final int blockBreakCooldown, blockBreakFromWave, reinforcementFromWave;

    // targeting
    public final boolean targetCreative, targetSpectators;

    // spawning
    public final boolean spawnerEnabled, daylightSpawn, boostNaturalPlacement;
    public final boolean ignoreGamerule, pressureCreative;
    public final int spawnIntervalTicks, spawnAttempts, ringMinSq, ringMaxSq, ringMin, ringMax;
    public final double surfaceChance, daySpawnFactor;

    // effects
    public final double effectProcChance;
    public final boolean effectsOnlyDuringWaves;
    public final int effectAmplifier;

    // precomputed per-wave tables (index 0 = wave 0/1 idle baseline … index maxWaves)
    private final double[] intelligence;      // [maxWaves+1]
    private final double[] speedCalm;         // [maxWaves+1]
    private final double[] speedWave;         // [maxWaves+1]
    private final double[] healthFlat;        // [maxWaves+1] uncapped (player-dependent cap applied at mutation)
    private final double[] attack;            // [maxWaves+1] already damage-capped
    private final double[] spawnCap;          // [maxWaves+1] clamped to spawning.capMax
    private final double[] reinforcementsCalm, reinforcementsWave; // [maxWaves+1]
    private final double[] kbr;               // [maxWaves+1] wave-only knockback resistance
    private final double[] reachScale;        // [maxWaves+1]
    private final double[] followCalm, followWave;   // [maxWaves+1] pre-intel-scale (scale applied once here too)
    private final double[] hearingCalm, hearingWave; // [maxWaves+1]
    private final int[] retargetCalm, retargetWave;  // [maxWaves+1] ticks
    private final int[] unseenCalm, unseenWave;      // [maxWaves+1] ticks
    private final int[] noiseCalm, noiseWave;        // [maxWaves+1] ticks
    private final boolean[] breacherGate;     // [maxWaves+1] designWave >= blockBreakFromWave

    private ZTSnapshot(int epoch) {
        this.epoch = epoch;
        // ---- scalars first (single config read each) ----
        this.enabled = ZTConfig.ENABLED.get();
        this.maxWaves = Math.max(1, ZTConfig.MAX_WAVES.get());
        this.chatAnnounce = ZTConfig.CHAT_ANNOUNCE.get();
        this.titleAnnounce = ZTConfig.TITLE_ANNOUNCE.get();

        this.intelligenceMax = ZTConfig.I_MAX.get();
        double iBase = ZTConfig.I_BASE.get();
        double iPerWave = ZTConfig.I_PER_WAVE.get();
        int unseenBase = ZTConfig.I_UNSEEN_MEMORY.get();
        int unseenPerWave = ZTConfig.I_UNSEEN_PER_WAVE.get();
        this.unseenMemoryBase = unseenBase;

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

        this.effectProcChance = ZTConfig.E_PROC_CHANCE.get();
        this.effectsOnlyDuringWaves = ZTConfig.E_ONLY_DURING_WAVES.get();
        this.effectAmplifier = ZTConfig.E_AMPLIFIER.get();

        // remaining zombie curve parameters (captured once, folded into arrays below)
        double baseSpeed = ZTConfig.Z_BASE_SPEED.get();
        double maxSpeed = ZTConfig.Z_MAX_SPEED.get();
        double speedPerWave = ZTConfig.Z_SPEED_PER_WAVE.get();
        double followBase = ZTConfig.Z_FOLLOW_RANGE.get();
        double followPerWave = ZTConfig.Z_FOLLOW_PER_WAVE.get();
        double followCap = ZTConfig.Z_FOLLOW_CAP.get();
        double followWaveBonus = ZTConfig.Z_FOLLOW_WAVE_BONUS.get();
        double healthBase = ZTConfig.Z_HEALTH_BASE_HEARTS.get();
        double healthPerWave = ZTConfig.Z_HEALTH_PER_WAVE_HEARTS.get();
        double kbrPerWave = ZTConfig.Z_KBR_PER_WAVE.get();
        double reinforcementPerWave = ZTConfig.Z_REINFORCEMENT_PER_WAVE.get();
        double hearingBase = ZTConfig.Z_HEARING_RADIUS.get();
        double hearingPerWave = ZTConfig.Z_HEARING_PER_WAVE.get();
        double hearingCap = ZTConfig.Z_HEARING_CAP.get();
        int noiseBase = ZTConfig.Z_NOISE_COOLDOWN_TICKS.get();
        double damagePerWave = ZTConfig.Z_DAMAGE_PER_WAVE_HEARTS.get();
        int capPerPlayer = ZTConfig.S_CAP_PER_PLAYER.get();
        double capPerWave = ZTConfig.S_CAP_PER_WAVE.get();
        int capMax = ZTConfig.S_CAP_MAX.get();

        // ---- wave [] tables ----
        int n = this.maxWaves;
        this.calmTicksByWave = new long[n + 1];
        this.durationTicksByWave = new long[n + 1];
        Map<Integer, Integer> intervals = ZTConfig.intervalOverrides();
        Map<Integer, Integer> durations = ZTConfig.durationOverrides();
        double firstMinutes = ZTConfig.FIRST_WAVE_MINUTES.get();
        double incrementMinutes = ZTConfig.WAVE_INCREMENT_MINUTES.get();
        for (int w = 1; w <= n; w++) {
            Integer calm = intervals.get(w);
            this.calmTicksByWave[w] = Math.max(100L, (calm != null ? (long) calm : ZTConfig.DEFAULT_CALM_SECONDS) * 20L);
            Integer dur = durations.get(w);
            this.durationTicksByWave[w] = dur != null
                    ? Math.max(100L, dur.longValue() * 20L)
                    : Math.max(100L, Math.round((firstMinutes + (w - 1) * incrementMinutes) * 1200.0D));
        }
        this.calmTicksByWave[0] = this.calmTicksByWave[1];
        this.durationTicksByWave[0] = this.durationTicksByWave[1];

        int size = n + 1;
        this.intelligence = new double[size];
        this.speedCalm = new double[size];
        this.speedWave = new double[size];
        this.healthFlat = new double[size];
        this.attack = new double[size];
        this.spawnCap = new double[size];
        this.reinforcementsCalm = new double[size];
        this.reinforcementsWave = new double[size];
        this.kbr = new double[size];
        this.reachScale = new double[size];
        this.followCalm = new double[size];
        this.followWave = new double[size];
        this.hearingCalm = new double[size];
        this.hearingWave = new double[size];
        this.retargetCalm = new int[size];
        this.retargetWave = new int[size];
        this.unseenCalm = new int[size];
        this.unseenWave = new int[size];
        this.noiseCalm = new int[size];
        this.noiseWave = new int[size];
        this.breacherGate = new boolean[size];

        double intelMaxLocal = this.intelligenceMax;
        for (int w = 0; w <= n; w++) {
            double dw = w * (50.0D / n); // designWave, no config read needed here
            double intel = Math.min(intelMaxLocal, iBase + Math.max(0, dw) * iPerWave);
            this.intelligence[w] = intel;
            double scale = 0.75D + 0.25D * intel;
            this.reachScale[w] = scale;

            double calmSpd = baseSpeed + Math.max(0, dw - 1) * speedPerWave;
            this.speedCalm[w] = Math.min(maxSpeed, calmSpd);
            this.speedWave[w] = Math.min(maxSpeed, calmSpd + frenzySpeedBoost * frenzyIntensity);

            this.healthFlat[w] = (healthBase + Math.max(0, dw) * healthPerWave) * 2.0D;
            this.attack[w] = Math.min(maxDamage, 3.0D + Math.max(0, dw) * damagePerWave * 2.0D);
            this.spawnCap[w] = Math.min(capMax, capPerPlayer + Math.max(0, w) * capPerWave);

            this.reinforcementsCalm[w] = dw >= reinforcementFromWave
                    ? Math.min(reinforcementCap, 0.1D + dw * reinforcementPerWave) : 0.1D;
            this.reinforcementsWave[w] = dw >= reinforcementFromWave
                    ? Math.min(reinforcementCap, (0.1D + dw * reinforcementPerWave) * (1.0D + 0.5D * frenzyIntensity)) : 0.1D;
            this.kbr[w] = Math.min(0.4D, dw * kbrPerWave * frenzyIntensity);

            this.followCalm[w] = Math.min(followCap, (followBase + Math.max(0, dw) * followPerWave) * scale);
            this.followWave[w] = Math.min(followCap, (followBase + Math.max(0, dw) * followPerWave + followWaveBonus * frenzyIntensity) * scale);
            this.hearingCalm[w] = Math.min(hearingCap, (hearingBase + Math.max(0, dw) * hearingPerWave) * scale);
            this.hearingWave[w] = Math.min(hearingCap, (hearingBase + Math.max(0, dw) * hearingPerWave + frenzyHearingBonus * frenzyIntensity) * scale);

            this.retargetCalm[w] = Math.max(2, Math.min(40, (int) Math.round(14.0D - 2.0D * intel)));
            this.retargetWave[w] = Math.max(2, Math.min(40, (int) Math.round(14.0D - 2.0D * intel - 3.0D * frenzyIntensity)));

            int unseen = unseenBase + (int) Math.round(Math.max(0, dw) * unseenPerWave);
            this.unseenCalm[w] = Math.max(10, Math.min(600, unseen));
            this.unseenWave[w] = Math.max(10, Math.min(600, (int) Math.round(unseen * (1.0D + 0.5D * frenzyIntensity))));

            double brainFactor = Math.max(0.5D, 1.0D - 0.08D * intel);
            this.noiseCalm[w] = Math.max(5, (int) Math.round(noiseBase * brainFactor));
            this.noiseWave[w] = Math.max(5, (int) Math.round(noiseBase * brainFactor * frenzyNoiseFactor));

            this.breacherGate[w] = dw >= blockBreakFromWave;
        }
    }

    // ------------------------------------------------------------------ accessors
    private int clampWave(int wave) {
        return wave < 0 ? 0 : (wave > maxWaves ? maxWaves : wave);
    }

    /** Zombie max health for {@code wave}, capped at player + healthMaxAbovePlayer hearts. */
    public double healthFor(int wave, double playerMaxHealth) {
        double cap = Math.max(2.0D, playerMaxHealth) + healthMaxAbovePlayer * 2.0D;
        return Math.max(2.0D, Math.min(healthFlat[clampWave(wave)], cap));
    }

    /** Zombie attack-damage attribute for {@code wave} (already hard-capped). */
    public double attackFor(int wave) { return attack[clampWave(wave)]; }

    /** Movement speed for {@code wave}. */
    public double speedFor(int wave, boolean active) {
        int i = clampWave(wave);
        return active ? speedWave[i] : speedCalm[i];
    }

    /** Follow range for {@code wave}. */
    public double followFor(int wave, boolean active) {
        int i = clampWave(wave);
        return active ? followWave[i] : followCalm[i];
    }

    /** Hearing radius for {@code wave}. */
    public double hearingFor(int wave, boolean active) {
        int i = clampWave(wave);
        return active ? hearingWave[i] : hearingCalm[i];
    }

    /** Reinforcement-call chance for {@code wave}. */
    public double reinforcementsFor(int wave, boolean active) {
        int i = clampWave(wave);
        return active ? reinforcementsWave[i] : reinforcementsCalm[i];
    }

    /** Wave-only knockback resistance for {@code wave}. */
    public double kbrFor(int wave) { return kbr[clampWave(wave)]; }

    /** Alive-zombie cap per player for {@code wave}. */
    public double spawnCapFor(int wave) { return spawnCap[clampWave(wave)]; }

    /** Intelligence level for {@code wave}. */
    public double intelligenceFor(int wave) { return intelligence[clampWave(wave)]; }

    /** Hunt-goal re-check cadence (ticks) for {@code wave}. */
    public int retargetTicksFor(int wave, boolean active) {
        int i = clampWave(wave);
        return active ? retargetWave[i] : retargetCalm[i];
    }

    /** Lost-sight memory (ticks) for {@code wave}. */
    public int unseenTicksFor(int wave, boolean active) {
        int i = clampWave(wave);
        return active ? unseenWave[i] : unseenCalm[i];
    }

    /** Sense-engine cooldown (ticks) for {@code wave}. */
    public int noiseTicksFor(int wave, boolean active) {
        int i = clampWave(wave);
        return active ? noiseWave[i] : noiseCalm[i];
    }

    /** Design-wave gate for the block-breaking behaviour. */
    public boolean breacherGateFor(int wave) { return breacherGate[clampWave(wave)]; }

    /** Calm gap (ticks) before wave {@code nextWave} arrives. */
    public long calmTicksFor(int nextWave) { return calmTicksByWave[clampWave(Math.max(1, nextWave))]; }

    /** Duration (ticks) of wave {@code wave}. */
    public long durationTicksFor(int wave) { return durationTicksByWave[clampWave(Math.max(1, wave))]; }
}
