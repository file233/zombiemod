package dev.file233.zombietide.zombie;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.wave.WaveManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Spawn hygiene of the apocalypse.
 *
 * <p>Outside a wave the world is vanilla (plus wave-AI doctrine on zombies). <b>During</b> a
 * wave the filter enforces the design law: <i>only plain zombies flow from natural
 * spawning</i> —
 * <ul>
 *   <li>drowned / husk / zombie villagers are re-created as plain zombies (kept with the tiny
 *       configured chance),</li>
 *   <li>baby zombies grow up on the spot (kept with the tiny configured chance),</li>
 *   <li>skeletons, creepers, spiders, witches and every other hostile spawn are denied
 *       (kept with the tiny configured chance).</li>
 * </ul>
 *
 * <p>It also flips the daylight rule for the vanilla spawner: while a wave runs, natural
 * zombie spawns ignore light levels entirely.
 */
public final class SpawnFilter {
    private SpawnFilter() {}

    private static boolean naturalish(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION
                || type == MobSpawnType.REINFORCEMENT || type == MobSpawnType.JOCKEY;
    }

    // ------------------------------------------------------------------ finalize spawn
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!ZTConfig.enabled()) return;
        ServerLevelAccessor level = event.getLevel();
        if (!ZTConfig.dimensionAllowed(level.getLevel().dimension())) return;
        if (!WaveManager.isWaveActive()) return;
        if (!(event.getEntity() instanceof Enemy)) return;
        if (!naturalish(event.getSpawnType())) return;

        var random = level.getRandom();

        if (event.getEntity() instanceof Zombie zombie) {
            if (zombie.getType() == EntityType.ZOMBIE) {
                // babies almost never slip through wave spawn hygiene
                if (zombie.isBaby() && random.nextFloat() >= ZTConfig.Z_BABY_KEEP_CHANCE.get()) {
                    zombie.setBaby(false);
                }
            } else {
                // drowned / husk / zombie villagers → plain horde members
                if (random.nextFloat() >= ZTConfig.Z_VARIANT_KEEP_CHANCE.get()) {
                    if (ZTConfig.Z_CONVERT_VARIANTS.get()) {
                        Zombie plain = EntityType.ZOMBIE.create(level.getLevel());
                        if (plain != null) {
                            plain.moveTo(zombie.getX(), zombie.getY(), zombie.getZ(),
                                    zombie.getYRot(), zombie.getXRot());
                            plain.finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), null);
                            level.getLevel().addFreshEntity(plain);
                        }
                    }
                    event.setSpawnCancelled(true);
                }
            }
            return;
        }

        // every other hostile mob gets swallowed by the tide
        if (random.nextFloat() >= ZTConfig.Z_NON_ZOMBIE_KEEP_CHANCE.get()) {
            event.setSpawnCancelled(true);
        }
    }

    // ------------------------------------------------------------------ join level (post-finalize, incl. chunk loads)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!ZTConfig.enabled()) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevelAccessor)) return;
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!ZTConfig.dimensionAllowed(zombie.level().dimension())) return;
        // runs after vanilla finalizeSpawn → also strips any armor vanilla just equipped
        ZombieMutation.apply(zombie, event.loadedFromDisk());
    }

    // ------------------------------------------------------------------ daylight for the vanilla spawner
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!ZTConfig.enabled() || !ZTConfig.S_BOOST_NATURAL_PLACEMENT.get()) return;
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        if (event.getEntityType() != EntityType.ZOMBIE) return;
        if (!ZTConfig.dimensionAllowed(event.getLevel().getLevel().dimension())) return;
        if (!WaveManager.isWaveActive()) return;
        // ignores light & monster rules: the tide spawns under the noon sun
        event.setResult(Event.Result.ALLOW);
    }

    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!ZTConfig.enabled() || !ZTConfig.S_BOOST_NATURAL_PLACEMENT.get()) return;
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        if (event.getEntity() == null || event.getEntity().getType() != EntityType.ZOMBIE) return;
        if (!ZTConfig.dimensionAllowed(event.getLevel().getLevel().dimension())) return;
        if (!WaveManager.isWaveActive()) return;
        event.setResult(MobSpawnEvent.PositionCheck.Result.SUCCEED);
    }
}
