package dev.file233.zombietide.zombie;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.config.ZTSnapshot;
import dev.file233.zombietide.wave.WaveManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
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
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled) return;
        // cheap gates first: instance checks & phase flags cost nothing, registry/dimension
        // lookups only run when the apocalypse is actually raging
        if (!(event.getEntity() instanceof Enemy)) return;
        if (!naturalish(event.getSpawnType())) return;
        if (!WaveManager.isWaveActive()) return;
        ServerLevelAccessor level = event.getLevel();
        if (!ZTConfig.dimensionAllowed(level.getLevel().dimension())) return;

        var random = level.getRandom();

        if (event.getEntity() instanceof Zombie zombie) {
            if (zombie.getType() == EntityType.ZOMBIE) {
                // babies almost never slip through wave spawn hygiene
                if (zombie.isBaby() && random.nextFloat() >= snap.babyKeepChance) {
                    zombie.setBaby(false);
                }
            } else {
                // drowned / husk / zombie villagers → plain horde members
                if (random.nextFloat() >= snap.variantKeepChance) {
                    if (snap.convertVariants) {
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
        if (random.nextFloat() >= snap.nonZombieKeepChance) {
            event.setSpawnCancelled(true);
        }
    }

    // ------------------------------------------------------------------ join level (post-finalize, incl. chunk loads)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!ZTSnapshot.get().enabled) return;
        if (!(event.getLevel() instanceof ServerLevelAccessor)) return;
        if (!ZTConfig.dimensionAllowed(zombie.level().dimension())) return;
        // runs after vanilla finalizeSpawn → also strips any armor vanilla just equipped
        ZombieMutation.apply(zombie, event.loadedFromDisk());
    }

    // ------------------------------------------------------------------ daylight for the vanilla spawner
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled || !snap.boostNaturalPlacement) return;
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        if (event.getEntityType() != EntityType.ZOMBIE) return;
        if (!WaveManager.isWaveActive()) return;
        if (!ZTConfig.dimensionAllowed(event.getLevel().getLevel().dimension())) return;
        // ignores light & monster rules: the tide spawns under the noon sun
        event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
    }

    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled || !snap.boostNaturalPlacement) return;
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        if (event.getEntity() == null || event.getEntity().getType() != EntityType.ZOMBIE) return;
        if (!WaveManager.isWaveActive()) return;
        if (!ZTConfig.dimensionAllowed(event.getLevel().getLevel().dimension())) return;
        event.setResult(MobSpawnEvent.PositionCheck.Result.SUCCEED);
    }
}
