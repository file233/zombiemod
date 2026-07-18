package dev.file233.zombietide.zombie;

import org.jetbrains.annotations.Nullable;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.registry.ZTAttachments;
import dev.file233.zombietide.wave.WaveManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The zombies' ears. Every careless thing you do — sprinting, mining, eating, opening a
 * door, shooting, detonating the quarry — rings a dinner bell whose volume is the
 * configured hearing radius multiplied by how loud the action is. Zombies with the scent
 * either lock onto you directly (no line of sight needed) or shamble toward the echo.
 *
 * <p>The system is deliberately cheap: movement noise is sampled at 2.5 Hz per player,
 * everything else rides discrete events, and each zombie has its own reaction cooldown.
 */
public final class SenseEngine {
    private SenseEngine() {}

    // relative loudness of player actions (multiplier of the hearing radius)
    private static final double LOUD_SPRINT = 0.75D;
    private static final double LOUD_WALK = 0.45D;
    private static final double LOUD_SWIM = 0.50D;
    private static final double LOUD_SNEAK = 0.15D;
    private static final double LOUD_JUMP = 0.50D;
    private static final double LOUD_BREAK_BLOCK = 1.00D;
    private static final double LOUD_PLACE_BLOCK = 0.55D;
    private static final double LOUD_MELEE = 0.70D;
    private static final double LOUD_PROJECTILE = 0.85D;
    private static final double LOUD_EAT = 0.40D;
    private static final double LOUD_INTERACT = 0.30D;
    private static final double LOUD_EXPLOSION = 3.00D;

    // ------------------------------------------------------------------ movement noise (server side)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ZTConfig.enabled()) return;
        if ((player.tickCount & 7) != 2) return; // sampled at 2.5 Hz
        if (player.isSpectator() || !player.isAlive()) return;

        double loudness;
        if (player.isSprinting() && player.onGround()) loudness = LOUD_SPRINT;
        else if (player.isCrouching()) loudness = LOUD_SNEAK;
        else if (player.isSwimming()) loudness = LOUD_SWIM;
        else if (player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D) loudness = LOUD_WALK;
        else return; // standing still is silent

        alert(player, player.position(), loudness, player);
    }

    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ZTConfig.enabled()) {
            alert(player, player.position(), LOUD_JUMP, player);
        }
    }

    // ------------------------------------------------------------------ action noise (events)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!ZTConfig.enabled()) return;
        if (event.getLevel() instanceof ServerLevel level && event.getPlayer() != null) {
            alert(level, Vec3.atCenterOf(event.getPos()), LOUD_BREAK_BLOCK, event.getPlayer());
        }
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!ZTConfig.enabled()) return;
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Player player) {
            alert(level, Vec3.atCenterOf(event.getPos()), LOUD_PLACE_BLOCK, player);
        }
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (!ZTConfig.enabled()) return;
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() != null) {
            alert(player, event.getTarget().position(), LOUD_MELEE, player);
        }
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!ZTConfig.enabled()) return;
        if (!(event.getEntity() instanceof Projectile projectile)) return;
        if (!(projectile.getOwner() instanceof Player player)) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        alert(level, event.getRayTraceResult().getLocation(), LOUD_PROJECTILE, player);
    }

    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!ZTConfig.enabled()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            alert(player, player.position(), LOUD_EAT, player);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ZTConfig.enabled()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            alert(player, Vec3.atCenterOf(event.getPos()), LOUD_INTERACT, player);
        }
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!ZTConfig.enabled()) return;
        if (event.getLevel() instanceof ServerLevel level) {
            alert(level, event.getExplosion().center(), LOUD_EXPLOSION, null);
        }
    }

    // ------------------------------------------------------------------ the bell
    private static void alert(LivingEntity sourceEntity, Vec3 pos, double relativeRadius, @Nullable Player culprit) {
        if (!(sourceEntity.level() instanceof ServerLevel level)) return;
        alert(level, pos, relativeRadius, culprit);
    }

    /**
     * Rings the dinner bell. {@code culprit} (nullable) is who made the noise; zombies
     * without a target will lock onto them, or otherwise converge on the echo itself.
     */
    public static void alert(ServerLevel level, Vec3 pos, double relativeRadius, @Nullable Player culprit) {
        if (!ZTConfig.enabled()) return;
        if (!ZTConfig.dimensionAllowed(level.dimension())) return;

        double radius = Math.min(160.0D, ZTConfig.hearingRadius(WaveManager.currentWave()) * relativeRadius);
        if (radius < 1.0D) return;

        AABB box = new AABB(
                pos.x - radius, pos.y - radius * 0.75D, pos.z - radius,
                pos.x + radius, pos.y + radius * 0.75D, pos.z + radius);
        long now = level.getGameTime();
        int cooldownTicks = Math.max(5, ZTConfig.Z_NOISE_COOLDOWN_TICKS.get());

        for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, box)) {
            if (zombie.distanceToSqr(pos) > radius * radius) continue;
            long lastReact = zombie.getData(ZTAttachments.NOISE_COOLDOWN);
            if (now - lastReact < cooldownTicks) continue;
            zombie.setData(ZTAttachments.NOISE_COOLDOWN, now);

            if (zombie.getTarget() == null) {
                if (culprit != null && culprit.isAlive() && !culprit.isSpectator() && !culprit.isCreative()) {
                    zombie.setTarget(culprit);
                    zombie.setAggressive(true);
                } else {
                    zombie.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.0D);
                }
            }
        }
    }
}
