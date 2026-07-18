package dev.file233.zombietide.zombie;

import org.jetbrains.annotations.Nullable;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.config.ZTSnapshot;
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

/**
 * Wall-chewer behaviour granted to a share of zombies from wave 20 onward.
 *
 * <p>When the zombie's prey is scented but unreachable (navigation stalls), the ghoul
 * picks the block standing between it and dinner, audibly gnaws through it with vanilla
 * crack-progress particles/particles sound, and keeps going. Pure vanilla mechanics
 * (mimics a player mining, obeys {@code mobGriefing}), no fake physics — it just never
 * stops. A door, a window, a wooden wall: allus just delays.
 *
 * <p><b>Performance:</b> the 2Hz wall scans reuse one mutable scratch position (no
 * BlockPos/Vec3 churn), hardness gates come from the snapshot, and the blacklist set is
 * only consulted after every cheaper test has passed.
 */
public class BlockBreakGoal extends Goal {
    /** Vertical probe order: feet level, head level, ceiling, floor — mirrors the old fixed candidates. */
    private static final int[] SCAN_OFFSETS = {0, 1, 2, -1};

    private final Zombie mob;
    @Nullable
    private BlockPos targetPos;
    private int digTicks;
    private int needTicks = 60;
    private int lastStage = -1;
    private int cooldown;
    private final int stagger;
    /** Scratch for scan probes — never escapes (winners are copied via {@link BlockPos#immutable()}). */
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    public BlockBreakGoal(Zombie mob) {
        this.mob = mob;
        this.stagger = mob.getRandom().nextInt(10);
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (cooldown > 0) { cooldown--; return false; }
        if ((mob.tickCount + stagger) % 10 != 0) return false; // scan at 2Hz, staggered
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled) return false;
        if (!ZTConfig.dimensionAllowed(mob.level().dimension())) return false;
        int wave = WaveManager.currentWave();
        if (!snap.breacherGateFor(wave)) return false;
        if (snap.blockBreakOnlyWaves && !WaveManager.isWaveActive()) return false;
        if (!mob.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;

        LivingEntity prey = mob.getTarget();
        if (prey == null || !prey.isAlive()) return false;
        // navigation gave up → something physical is between the zombie and dinner
        if (!mob.getNavigation().isDone() && mob.distanceToSqr(prey) < 9.0D) return false;

        BlockPos candidate = findBreakableToward(prey, snap);
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
        return isBreakable(targetPos, ZTSnapshot.get());
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
        // fixed point of the block's center — no Vec3 allocation per tick
        double cx = targetPos.getX() + 0.5D;
        double cy = targetPos.getY() + 0.5D;
        double cz = targetPos.getZ() + 0.5D;
        double dx = mob.getX() - cx, dy = mob.getY() - cy, dz = mob.getZ() - cz;

        // stroll into chewing range, then stay planted
        if (dx * dx + dy * dy + dz * dz > 2.6D) {
            mob.getNavigation().moveTo(cx, cy, cz, 1.0D);
        } else {
            mob.getNavigation().stop();
        }
        mob.getLookControl().setLookAt(cx, cy, cz);

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
            cooldown = ZTSnapshot.get().blockBreakCooldown;
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
    private BlockPos findBreakableToward(LivingEntity prey, ZTSnapshot snap) {
        double dirX = prey.getX() - mob.getX();
        double dirZ = prey.getZ() - mob.getZ();
        double lenSq = dirX * dirX + dirZ * dirZ;
        if (lenSq < 1.0E-4D) return null;
        double invLen = 1.0D / Math.sqrt(lenSq);
        dirX *= invLen;
        dirZ *= invLen;

        int bx = Mth.floor(mob.getX() + dirX * 1.6D);
        int by = Mth.floor(mob.getY());
        int bz = Mth.floor(mob.getZ() + dirZ * 1.6D);
        for (int offset : SCAN_OFFSETS) {
            scratch.set(bx, by + offset, bz);
            if (isBreakable(scratch, snap)) return scratch.immutable();
        }
        // prey hugging the wall: chew the block they lean on
        if (mob.distanceToSqr(prey) <= 12.0D) {
            BlockPos preyPos = prey.blockPosition();
            if (isBreakable(preyPos, snap)) return preyPos;
            BlockPos above = preyPos.above();
            if (isBreakable(above, snap)) return above;
        }
        return null;
    }

    private boolean isBreakable(BlockPos pos, ZTSnapshot snap) {
        BlockState state = mob.level().getBlockState(pos);
        if (state.isAir() || state.liquid()) return false;
        float hardness = state.getDestroySpeed(mob.level(), pos);
        if (hardness < 0.0F) return false; // bedrock & friends
        if (hardness > snap.blockBreakMaxHardness) return false;
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
