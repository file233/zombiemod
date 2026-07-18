package dev.file233.zombietide.zombie;

import java.util.ArrayList;
import java.util.List;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.config.ZTSnapshot;
import dev.file233.zombietide.wave.WaveManager;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Combat law of the apocalypse:
 * <ul>
 *   <li>a zombie never lands more than <b>2 hearts</b> (configurable) on a player, ever;</li>
 *   <li>some bites are infected — a random harmful effect out of the configurable pool,
 *       with deadlier cocktails unlocking as the wave count climbs;</li>
 *   <li>during a wave, sunlight is fiction: burning zombies are instantly doused.</li>
 * </ul>
 *
 * <p><b>Performance:</b> the per-entity tick gate (sun dousing) fires for EVERY entity in
 * the world, so it reads exactly two snapshot primitives before doing anything else. The
 * bite-effect pool for the current wave is rebuilt only when the wave or config epoch
 * changes — a hit then costs one dice roll and one weighted pick, no list churn.
 */
public final class CombatRules {
    private CombatRules() {}

    // bite-effect pool, cached per (config epoch, wave): hits rebuild it ~never
    private static int poolEpoch = -1;
    private static int poolWave = -1;
    private static int poolMaxWaves = -1;
    private static List<ZTConfig.EffectRoll> pool = List.of();
    private static int poolWeight = 0;

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getSource().getEntity() instanceof Zombie)) return;

        // ---- the unbreakable damage cap (2 hearts by design) ----
        float cap = (float) snap.maxDamage;
        if (event.getAmount() > cap) {
            event.setAmount(cap);
        }

        // ---- infected bites ----
        if (snap.effectsOnlyDuringWaves && !WaveManager.isWaveActive()) return;
        var rand = player.getRandom();
        if (rand.nextFloat() >= snap.effectProcChance) return;

        int wave = WaveManager.currentWave();
        if (snap.epoch != poolEpoch || wave != poolWave || snap.maxWaves != poolMaxWaves) {
            rebuildPool(snap, wave);
        }
        if (poolWeight <= 0) return;

        int ticket = rand.nextInt(poolWeight);
        ZTConfig.EffectRoll chosen = pool.get(0);
        for (ZTConfig.EffectRoll roll : pool) {
            ticket -= roll.weight();
            if (ticket < 0) { chosen = roll; break; }
        }

        Holder<MobEffect> holder = ZTConfig.resolveEffect(chosen.id());
        if (holder == null) return;
        int span = Math.max(0, chosen.maxSec() - chosen.minSec());
        int seconds = chosen.minSec() + (span > 0 ? rand.nextInt(span + 1) : 0);
        int amplifier = Math.min(3, Math.max(0, snap.effectAmplifier));
        player.addEffect(new MobEffectInstance(holder, seconds * 20, amplifier, false, true, true));
    }

    private static void rebuildPool(ZTSnapshot snap, int wave) {
        double designWave = wave * (50.0D / snap.maxWaves);
        List<ZTConfig.EffectRoll> fresh = new ArrayList<>();
        int weight = 0;
        for (ZTConfig.EffectRoll roll : ZTConfig.effectRolls()) {
            if (designWave >= roll.minWave()) {
                fresh.add(roll);
                weight += roll.weight();
            }
        }
        pool = List.copyOf(fresh);
        poolWeight = weight;
        poolEpoch = snap.epoch;
        poolWave = wave;
        poolMaxWaves = snap.maxWaves;
    }

    /** Sun is not a weapon while the tide is in. */
    public static void onEntityTick(EntityTickEvent.Post event) {
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled || !snap.noBurnInWaves) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;
        if (!WaveManager.isWaveActive()) return;
        if (!ZTConfig.dimensionAllowed(zombie.level().dimension())) return;
        if (zombie.getRemainingFireTicks() > 0) {
            zombie.clearFire();
        }
    }
}
