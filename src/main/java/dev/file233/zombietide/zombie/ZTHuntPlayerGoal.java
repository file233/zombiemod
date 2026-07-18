package dev.file233.zombietide.zombie;

import dev.file233.zombietide.config.ZTConfig;
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
 * exactly like survivors — it just cannot chew through their invulnerability. Selector and
 * persistence both run through {@link ZTConfig#isHuntable(Player)}, because vanilla's own
 * {@code canContinueToUse()} hard-drops any creative/spectator target.
 *
 * <p>Registered in addition to (never instead of) the vanilla targeting goals, so all
 * vanilla niceties (re-targeting, zombie villagers, etc.) keep working untouched.
 */
public class ZTHuntPlayerGoal extends NearestAttackableTargetGoal<Player> {
    public ZTHuntPlayerGoal(Mob mob, int priorityInterval) {
        // mustSee = false → walls do not save you
        super(mob, Player.class, priorityInterval, false, false, null);
    }

    @Override
    public boolean canUse() {
        if (!ZTConfig.enabled() || !ZTConfig.Z_HUNT_WITHOUT_SIGHT.get()) return false;
        if (!ZTConfig.dimensionAllowed(this.mob.level().dimension())) return false;
        return super.canUse();
    }

    /**
     * Own acquisition pass: vanilla's findTarget() silently skips creative players; the
     * tide does not. Picks the nearest huntable player inside the (wave-fed) follow range.
     */
    @Override
    protected void findTarget() {
        double range = this.getFollowDistance();
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : this.mob.level().players()) {
            if (!ZTConfig.isHuntable(player)) continue;
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
        if (!ZTConfig.enabled() || !ZTConfig.Z_HUNT_WITHOUT_SIGHT.get()) return false;
        LivingEntity target = this.mob.getTarget() != null ? this.mob.getTarget() : this.target;
        if (target == null || !target.isAlive()) return false;
        if (target instanceof Player player && !ZTConfig.isHuntable(player)) {
            // became unhuntable (e.g. config flipped or switched to spectator) — release
            this.mob.setTarget(null);
            return false;
        }
        double range = this.getFollowDistance() * 1.15D; // small hysteresis band
        return this.mob.distanceToSqr(target) <= range * range;
    }
}
