package dev.file233.zombietide.zombie;

import java.util.ArrayList;
import java.util.List;

import dev.file233.zombietide.config.ZTConfig;
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
 */
public final class CombatRules {
    private CombatRules() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!ZTConfig.enabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getSource().getEntity() instanceof Zombie)) return;

        // ---- the unbreakable damage cap (2 hearts by design) ----
        float cap = (float) ZTConfig.maxZombieDamage();
        if (event.getAmount() > cap) {
            event.setAmount(cap);
        }

        // ---- infected bites ----
        if (ZTConfig.E_ONLY_DURING_WAVES.get() && !WaveManager.isWaveActive()) return;
        var rand = player.getRandom();
        if (rand.nextFloat() >= ZTConfig.E_PROC_CHANCE.get()) return;

        int wave = WaveManager.currentWave();
        List<ZTConfig.EffectRoll> pool = new ArrayList<>();
        int totalWeight = 0;
        for (ZTConfig.EffectRoll roll : ZTConfig.effectRolls()) {
            if (wave >= roll.minWave()) {
                pool.add(roll);
                totalWeight += roll.weight();
            }
        }
        if (pool.isEmpty() || totalWeight <= 0) return;

        int ticket = rand.nextInt(totalWeight);
        ZTConfig.EffectRoll chosen = pool.get(0);
        for (ZTConfig.EffectRoll roll : pool) {
            ticket -= roll.weight();
            if (ticket < 0) { chosen = roll; break; }
        }

        Holder<MobEffect> holder = ZTConfig.resolveEffect(chosen.id());
        if (holder == null) return;
        int span = Math.max(0, chosen.maxSec() - chosen.minSec());
        int seconds = chosen.minSec() + (span > 0 ? rand.nextInt(span + 1) : 0);
        int amplifier = Math.min(3, Math.max(0, ZTConfig.E_AMPLIFIER.get()));
        player.addEffect(new MobEffectInstance(holder, seconds * 20, amplifier, false, true, true));
    }

    /** Sun is not a weapon while the tide is in. */
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!ZTConfig.enabled() || !ZTConfig.Z_NO_BURN_IN_WAVES.get()) return;
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
