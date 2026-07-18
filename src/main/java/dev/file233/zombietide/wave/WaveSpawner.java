package dev.file233.zombietide.wave;

import dev.file233.zombietide.config.ZTConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Wave spawn engine. While a wave runs it keeps horde pressure on every player using
 * vanilla's own natural-spawn placement rules ({@link NaturalSpawner#isSpawnPositionOk}),
 * but with two apocalypse twists: it also works in full daylight, and zombies spawn even
 * at high noon — exactly like the design brief demands.
 *
 * <p>Per player and cycle it picks a few candidate positions on a ring (24–48 blocks by
 * default, vanilla's natural spawn band), checks them like NaturalSpawner would, and
 * materializes one plain zombie. All numbers are config-driven for performance tuning.
 */
public final class WaveSpawner {
    private WaveSpawner() {}

    public static void tick(MinecraftServer server, int wave) {
        if (!ZTConfig.S_ENABLED.get()) return;
        for (var dimKey : ZTConfig.dimensions()) {
            ServerLevel level = server.getLevel(dimKey);
            if (level == null) continue;
            if (level.getDifficulty() == Difficulty.PEACEFUL) continue;
            if (!ZTConfig.S_IGNORE_GAMERULE.get() && !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) continue;
            int interval = Math.max(5, ZTConfig.S_INTERVAL_TICKS.get());
            for (ServerPlayer player : level.players()) {
                // stagger players across ticks so we never scan everyone at once
                int salt = (player.getUUID().hashCode() & 0x7FFFFFFF) % interval;
                if (((level.getGameTime() + salt) % interval) != 0L) continue;
                spawnCycleFor(level, player, wave);
            }
        }
    }

    private static void spawnCycleFor(ServerLevel level, ServerPlayer player, int wave) {
        int cap = ZTConfig.zombieCap(wave);
        int nearby = level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(96.0D, 32.0D, 96.0D)).size();
        if (nearby >= cap) return;

        var random = level.getRandom();
        int attempts = ZTConfig.S_ATTEMPTS_PER_CYCLE.get();
        double minD = ZTConfig.S_RING_MIN.get();
        double maxD = Math.max(minD + 4.0D, ZTConfig.S_RING_MAX.get());

        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            double dist = minD + random.nextDouble() * (maxD - minD);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);

            BlockPos pos = random.nextDouble() < ZTConfig.S_SURFACE_CHANCE.get()
                    ? surfacePos(level, player, x, z)
                    : cavePos(level, player, x, z, random);
            if (pos == null) continue;
            if (!canSpawnAt(level, pos, minD)) continue;

            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) continue;
            zombie.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            // Triggers ZombieMutation via NeoForge's FinalizeSpawnEvent, then joins the world.
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
            level.addFreshEntity(zombie);
            return; // one reinforcement per player per cycle keeps the pacing eerie, not chaotic
        }
    }

    private static BlockPos surfacePos(ServerLevel level, ServerPlayer player, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight()) return null;
        // Don't rain zombies onto players deep underground — they'd never meet.
        if (Math.abs(y - player.getBlockY()) > 32) return null;
        return new BlockPos(x, y, z);
    }

    private static BlockPos cavePos(ServerLevel level, ServerPlayer player, int x, int z, net.minecraft.util.RandomSource random) {
        int baseY = Mth.clamp(player.getBlockY() + random.nextInt(7) - 3, level.getMinBuildHeight() + 2, level.getMaxBuildHeight() - 2);
        for (int dy = 0; dy < 8; dy++) {
            BlockPos ground = new BlockPos(x, baseY - dy, z);
            BlockPos feet = ground.above();
            BlockState floor = level.getBlockState(ground);
            if (floor.isAir() || floor.liquid()) continue;
            if (!floor.isFaceSturdy(level, ground, Direction.UP)) continue;
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
            return feet;
        }
        return null;
    }

    /** Mirrors vanilla natural-spawn checks, optionally dropping the light rule mid-apocalypse. */
    private static boolean canSpawnAt(ServerLevel level, BlockPos pos, double minDistance) {
        if (!level.getWorldBorder().isWithinBounds(pos)) return false;

        // the vanilla 24-block no-spawn bubble around EVERY player
        Vec3 center = pos.getCenter();
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(center) < minDistance * minDistance) return false;
        }

        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP)) return false;

        // the genuine vanilla placement predicate (collision, space, fluids…)
        if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, pos, EntityType.ZOMBIE)) return false;

        // mid-wave the sun is no longer a shield: daylight spawns are allowed by design
        if (!ZTConfig.S_DAYLIGHT_SPAWN.get() && level.getMaxLocalRawBrightness(pos) > 0) return false;

        return true;
    }
}
