package dev.file233.zombietide.zombie;

import dev.file233.zombietide.config.ZTConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * The "zombie nose" goal: acquires the nearest player <b>without requiring line of
 * sight</b>. Vanilla zombies only hunt what they can see; these ghouls hunt what they
 * can <i>hear and smell</i>, at the escalating follow-range the wave system feeds them.
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

    @Override
    public boolean canContinueToUse() {
        if (!ZTConfig.enabled() || !ZTConfig.Z_HUNT_WITHOUT_SIGHT.get()) return false;
        return super.canContinueToUse();
    }
}
