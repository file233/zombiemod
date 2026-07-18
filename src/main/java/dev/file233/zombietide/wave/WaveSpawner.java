package dev.file233.zombietide.wave;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.config.ZTSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Wave spawn engine. While a wave runs it keeps horde pressure on every player using
 * vanilla's own natural-spawn placement rules ({@link SpawnPlacementTypes#ON_GROUND}),
 * but with two apocalypse twists: it also works in full daylight, and zombies spawn even
 * at high noon — exactly like the design brief demands.
 *
 * <p>Per player and cycle it picks a few candidate positions on a ring (24–48 blocks by
 * default, vanilla's natural spawn band), checks them like NaturalSpawner would, and
 * materializes one plain zombie. All numbers are config-driven for performance tuning.
 *
 * <p><b>Performance doctrine:</b>
 * <ul>
 *   <li>every value comes from the {@link ZTSnapshot} — zero config-map walks per attempt;</li>
 *   <li>each player owns a tiny reusable 3-long cycle record (ring buffer, no map lookups,
 *       no per-cycle allocations, automatic eviction of departed players);</li>
 *   <li>position probing reuses one mutable {@link BlockPos};</li>
 *   <li>the day-time factor scales the <i>attempt count</i>, so daylight costs less CPU
 *       instead of rolling dice that throw work away.</li>
 * </ul>
 */
public final class WaveSpawner {
    private WaveSpawner() {}

    /**
     * Per-player spawn-cycle state: [nextCycleTick, aliveAtCycleStart, referenceEpoch].
     * Packed in parallel arrays indexed by a power-of-two ring over the player's hash —
     * constant memory, zero GC pressure, no WeakHashMap.
     */
    private static final int CYCLE_SLOTS = 32; // plenty: online players rarely exceed this
    private static final ServerPlayer[] CYCLE_OWNER = new ServerPlayer[CYCLE_SLOTS];
    private static final long[] CYCLE_STATE = new long[CYCLE_SLOTS * 3];

    // two mutable block-positions reused for every probe (server main thread only);
    // FLOOR stays separate so checking the block below never corrupts the candidate pos
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();
    private static final BlockPos.MutableBlockPos FLOOR = new BlockPos.MutableBlockPos();

    public static void tick(MinecraftServer server, int wave) {
        ZTSnapshot s = ZTSnapshot.get();
        if (!s.spawnerEnabled || s.spawnAttempts <= 0) return;
        for (var dimKey : ZTConfig.dimensions()) {
            ServerLevel level = server.getLevel(dimKey);
            if (level == null) continue;
            if (level.getDifficulty() == Difficulty.PEACEFUL) continue;
            if (!s.ignoreGamerule && !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) continue;
            int interval = Math.max(5, s.spawnIntervalTicks);
            long gameTime = level.getGameTime();
            for (ServerPlayer player : level.players()) {
                // spectators never attract the tide; creative players do when configured to
                if (player.isSpectator() || !player.isAlive()) continue;
                if (player.isCreative() && !s.pressureCreative) continue;
                // stagger players across ticks so we never scan everyone at once
                int salt = (player.hashCode() & 0x7FFFFFFF) % interval;
                if (((gameTime + salt) % interval) != 0L) continue;
                spawnCycleFor(level, player, wave, s, gameTime);
            }
        }
    }

    private static void spawnCycleFor(ServerLevel level, ServerPlayer player, int wave, ZTSnapshot s, long gameTime) {
        // daylight thins the ranks — by scaling the work, not by rolling dice that waste it
        double dayFactor = level.isDay() ? s.daySpawnFactor : 1.0D;
        if (dayFactor <= 0.0D) return;
        int attempts = (int) Math.round(s.spawnAttempts * Math.min(1.0D, dayFactor));
        if (attempts <= 0) attempts = 1; // noon still delivers the occasional shambler

        double cap = s.spawnCapFor(wave);
        var random = level.getRandom();
        int ringSlot = (player.hashCode() & 0x7FFFFFFF) & (CYCLE_SLOTS - 1);
        int base = ringSlot * 3;

        double minD = s.ringMin;
        double maxD = s.ringMax;

        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            double dist = minD + random.nextDouble() * (maxD - minD);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);

            BlockPos pos = random.nextDouble() < s.surfaceChance
                    ? surfacePos(level, player, x, z)
                    : cavePos(level, player, x, z, random);
            if (pos == null) continue;
            if (!canSpawnAt(level, pos, minD)) continue;

            // the no-spawn bubble test uses the per-player alive count refreshed per cycle
            if (aliveFor(level, player, ringSlot, base, gameTime) >= cap) return;

            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) continue;
            zombie.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            // Triggers ZombieMutation via NeoForge's FinalizeSpawnEvent, then joins the world.
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
            level.addFreshEntity(zombie);
            CYCLE_STATE[base + 1] += 1; // the fresh shambler counts immediately toward the cap
            return; // one reinforcement per player per cycle keeps the pacing eerie, not chaotic
        }
        // note: CYCLE_STATE[base+1] is refreshed lazily inside aliveFor() each cycle
    }

    /**
     * Alive-zombie count around a player, recomputed at most once per spawn cycle and
     * adjusted in between by the engine's own successful spawns. The expensive
     * {@code getEntitiesOfClass} box query therefore runs once per cycle, never per attempt.
     */
    private static long aliveFor(ServerLevel level, ServerPlayer player, int ringSlot, int base, long gameTime) {
        if (CYCLE_OWNER[ringSlot] != player || CYCLE_STATE[base] != gameTime) {
            CYCLE_OWNER[ringSlot] = player;
            CYCLE_STATE[base] = gameTime;
            CYCLE_STATE[base + 1] = level.getEntitiesOfClass(Zombie.class,
                    player.getBoundingBox().inflate(96.0D, 32.0D, 96.0D)).size();
        }
        return CYCLE_STATE[base + 1];
    }

    private static BlockPos surfacePos(ServerLevel level, ServerPlayer player, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight()) return null;
        // Don't rain zombies onto players deep underground — they'd never meet.
        if (Math.abs(y - player.getBlockY()) > 32) return null;
        POS.set(x, y, z);
        return POS;
    }

    private static BlockPos cavePos(ServerLevel level, ServerPlayer player, int x, int z, net.minecraft.util.RandomSource random) {
        int baseY = Mth.clamp(player.getBlockY() + random.nextInt(7) - 3, level.getMinBuildHeight() + 2, level.getMaxBuildHeight() - 2);
        for (int dy = 0; dy < 8; dy++) {
            POS.set(x, baseY - dy, z);
            BlockState floor = level.getBlockState(POS);
            if (floor.isAir() || floor.liquid()) continue;
            if (!floor.isFaceSturdy(level, POS, Direction.UP)) continue;
            POS.move(Direction.UP);
            if (!level.getBlockState(POS).isAir()) { POS.move(Direction.DOWN); continue; }
            POS.move(Direction.UP);
            if (!level.getBlockState(POS).isAir()) { POS.move(Direction.DOWN, 2); continue; }
            POS.move(Direction.DOWN); // feet level
            return POS;
        }
        return null;
    }

    /** Mirrors vanilla natural-spawn checks, optionally dropping the light rule mid-apocalypse. */
    private static boolean canSpawnAt(ServerLevel level, BlockPos pos, double minDistance) {
        if (!level.getWorldBorder().isWithinBounds(pos)) return false;

        // the vanilla 24-block no-spawn bubble around EVERY player (cheap virtual distance)
        double px = pos.getX() + 0.5D, py = pos.getY() + 0.5D, pz = pos.getZ() + 0.5D;
        double minSq = minDistance * minDistance;
        for (ServerPlayer other : level.players()) {
            double dx = other.getX() - px, dy = other.getY() - py, dz = other.getZ() - pz;
            if (dx * dx + dy * dy + dz * dz < minSq) return false;
        }

        FLOOR.set(pos.getX(), pos.getY() - 1, pos.getZ());
        BlockState floor = level.getBlockState(FLOOR);
        if (!floor.isFaceSturdy(level, FLOOR, Direction.UP)) return false;

        // the genuine vanilla placement predicate (collision, space, fluids…)
        if (!SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, pos, EntityType.ZOMBIE)) return false;

        // mid-wave the sun is no longer a shield: daylight spawns are allowed by design
        if (!ZTSnapshot.get().daylightSpawn && level.getMaxLocalRawBrightness(pos) > 0) return false;

        return true;
    }
}
