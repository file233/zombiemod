package dev.file233.zombietide.zombie;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.config.ZTSnapshot;
import dev.file233.zombietide.wave.WaveManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * The "zombie nose" goal: acquires the nearest player <b>without requiring line of
 * sight</b>. Vanilla zombies only hunt what they can see; these ghouls hunt what they
 * can <i>hear and smell</i>, at the escalating follow-range the wave system feeds them.
 *
 * <p>Unlike every stock targeting goal, this one treats players in <b>creative mode</b>
 * as prey too (config {@code targeting.targetCreativePlayers}): the horde stalks builders
 * exactly like survivors — it just cannot chew through their invulnerability.
 *
 * <p>Registered in addition to (never instead of) the vanilla targeting goals, so all
 * vanilla niceties (re-targeting, zombie villagers, etc.) keep working untouched.
 *
 * <p><b>Performance:</b> this goal's {@code canUse}/{@code canContinueToUse} run on every
 * zombie AI tick (10Hz), so it reads nothing but snapshot primitives, reuses the
 * config-values it computes for one tick in both stages, and checks prey against plain
 * live references — no stream, no allocation, no config walks in the steady state.
 */
public class ZTHuntPlayerGoal extends NearestAttackableTargetGoal<Player> {
    public ZTHuntPlayerGoal(Mob mob, int priorityInterval) {
        // mustSee = false → walls do not save you
        super(mob, Player.class, priorityInterval, false, false, null);
    }

    private boolean huntableGate(ZTSnapshot snap, Player player) {
        if (!player.isAlive()) return false;
        if (player.isSpectator()) return snap.targetSpectators;
        if (player.isCreative()) return snap.targetCreative;
        return true;
    }

    @Override
    public boolean canUse() {
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled || !snap.huntWithoutSight) return false;
        if (!ZTConfig.dimensionAllowed(this.mob.level().dimension())) return false;
        int wave = WaveManager.currentWave();
        boolean active = WaveManager.isWaveActive();
        // own retarget cadence: brains + wave-rage decide how twitchy the nose is
        int interval = snap.retargetTicksFor(wave, active);
        if (interval > 0 && this.mob.getRandom().nextInt(interval) != 0) return false;
        // and how long the scent lingers once sight is lost
        this.setUnseenMemoryTicks(snap.unseenTicksFor(wave, active));
        findTarget(snap);
        return this.target != null;
    }

    /**
     * Own acquisition pass: vanilla's findTarget() silently skips creative players; the
     * tide does not. Picks the nearest huntable player inside the (wave-fed) follow range.
     */
    private void findTarget(ZTSnapshot snap) {
        double range = this.getFollowDistance();
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : this.mob.level().players()) {
            if (!huntableGate(snap, player)) continue;
            double distance = player.distanceToSqr(this.mob);
            if (distance <= range * range && distance < bestDistance) {
                best = player;
                bestDistance = distance;
            }
        }
        this.target = best;
    }

    /**
     * Own persistence pass: vanilla's TargetGoal#canContinueToUse() drops any target the
     * moment it turns creative/spectator. Ours only lets go when the prey is dead, out of
     * (slightly stretched) range, or config no longer lets the zombie hunt that player.
     */
    @Override
    public boolean canContinueToUse() {
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled || !snap.huntWithoutSight) return false;
        LivingEntity target = this.mob.getTarget() != null ? this.mob.getTarget() : this.target;
        if (target == null || !target.isAlive()) return false;
        if (target instanceof Player player && !huntableGate(snap, player)) {
            // became unhuntable (e.g. config flipped or switched to spectator) — release
            this.mob.setTarget(null);
            return false;
        }
        double range = this.getFollowDistance() * 1.15D; // small hysteresis band
        return this.mob.distanceToSqr(target) <= range * range;
    }
}
