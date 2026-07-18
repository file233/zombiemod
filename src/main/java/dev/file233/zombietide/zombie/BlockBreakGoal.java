package dev.file233.zombietide.zombie;

import org.jetbrains.annotations.Nullable;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.wave.WaveManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Wall-chewer behaviour granted to a share of zombies from wave 20 onward.
 *
 * <p>When the zombie's prey is scented but unreachable (navigation stalls), the ghoul
 * picks the block standing between it and dinner, audibly gnaws through it with vanilla
 * crack-progress particles/particles sound, and keeps going. Pure vanilla mechanics
 * (mimics a player mining, obeys {@code mobGriefing}), no fake physics — it just never
 * stops. A door, a window, a wooden wall: allus just delays.
 */
public class BlockBreakGoal extends Goal {
    private final Zombie mob;
    @Nullable
    private BlockPos targetPos;
    private int digTicks;
    private int needTicks = 60;
    private int lastStage = -1;
    private int cooldown;
    private final int stagger;

    public BlockBreakGoal(Zombie mob) {
        this.mob = mob;
        this.stagger = mob.getRandom().nextInt(10);
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (cooldown > 0) { cooldown--; return false; }
        if ((mob.tickCount + stagger) % 10 != 0) return false; // scan at 2Hz, staggered
        if (!ZTConfig.enabled()) return false;
        if (!ZTConfig.dimensionAllowed(mob.level().dimension())) return false;
        if (!ZTConfig.blockBreakingAllowedNow(WaveManager.currentWave(), WaveManager.isWaveActive())) return false;
        if (!mob.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;

        LivingEntity prey = mob.getTarget();
        if (prey == null || !prey.isAlive()) return false;
        // navigation gave up → something physical is between the zombie and dinner
        if (!mob.getNavigation().isDone() && mob.distanceToSqr(prey) < 9.0D) return false;

        BlockPos candidate = findBreakableToward(prey);
        if (candidate == null) return false;
        this.targetPos = candidate;
        this.needTicks = ticksFor(stateAt(candidate));
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPos == null) return false;
        LivingEntity prey = mob.getTarget();
        if (prey == null || !prey.isAlive()) return false;
        return isBreakable(targetPos);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        digTicks = 0;
        lastStage = -1;
    }

    @Override
    public void tick() {
        if (targetPos == null || !(mob.level() instanceof ServerLevel server)) return;
        Vec3 center = targetPos.getCenter();

        // stroll into chewing range, then stay planted
        if (mob.distanceToSqr(center) > 2.6D) {
            mob.getNavigation().moveTo(center.x, center.y, center.z, 1.0D);
        } else {
            mob.getNavigation().stop();
        }
        mob.getLookControl().setLookAt(center);

        digTicks++;
        if (digTicks % 5 == 0) mob.swing(InteractionHand.MAIN_HAND);

        // audibly scrape every second
        if (digTicks % 20 == 0) {
            BlockState state = server.getBlockState(targetPos);
            server.playSound(null, targetPos, state.getSoundType().getHitSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.9F);
        }

        int stage = Mth.clamp((int) Math.floor(digTicks * 10.0D / needTicks), 0, 9);
        if (stage != lastStage) {
            lastStage = stage;
            server.destroyBlockProgress(mob.getId(), targetPos, stage);
        }

        if (digTicks >= needTicks) {
            BlockState done = server.getBlockState(targetPos);
            server.levelEvent(2001, targetPos, Block.getId(done)); // particles + break sound
            server.destroyBlock(targetPos, false, mob);            // no drops: apocalypse, not a quarry
            cooldown = ZTConfig.Z_BLOCK_BREAK_COOLDOWN.get();
            targetPos = null;
        }
    }

    @Override
    public void stop() {
        if (targetPos != null && mob.level() instanceof ServerLevel server) {
            server.destroyBlockProgress(mob.getId(), targetPos, -1);
        }
        targetPos = null;
        lastStage = -1;
    }

    // ------------------------------------------------------------------ selection
    @Nullable
    private BlockPos findBreakableToward(LivingEntity prey) {
        Vec3 dir = prey.position().subtract(mob.position());
        dir = new Vec3(dir.x, 0.0D, dir.z);
        if (dir.lengthSqr() < 1.0E-4D) return null;
        dir = dir.normalize();

        BlockPos base = BlockPos.containing(
                mob.getX() + dir.x * 1.6D, mob.getY(), mob.getZ() + dir.z * 1.6D);
        BlockPos[] candidates = { base, base.above(), base.above(2), base.below() };
        for (BlockPos pos : candidates) {
            if (isBreakable(pos)) return pos;
        }
        // prey hugging the wall: chew the block they lean on
        if (mob.distanceToSqr(prey) <= 12.0D) {
            BlockPos preyPos = prey.blockPosition();
            if (isBreakable(preyPos)) return preyPos;
            if (isBreakable(preyPos.above())) return preyPos.above();
        }
        return null;
    }

    private boolean isBreakable(BlockPos pos) {
        BlockState state = mob.level().getBlockState(pos);
        if (state.isAir() || state.liquid()) return false;
        float hardness = state.getDestroySpeed(mob.level(), pos);
        if (hardness < 0.0F) return false; // bedrock & friends
        if (hardness > ZTConfig.Z_BLOCK_BREAK_MAX_HARDNESS.get()) return false;
        return !ZTConfig.breakBlacklist().contains(state.getBlock());
    }

    private BlockState stateAt(BlockPos pos) {
        return mob.level().getBlockState(pos);
    }

    private static int ticksFor(BlockState state) {
        float hardness = Math.max(0.0F, state.getDestroySpeed(null, null));
        // glass ~0.3s, planks ~1.5s, stone-bricks ~3s at default settings
        return Mth.ceil(12.0D + hardness * 22.0D);
    }
}
