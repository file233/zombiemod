package dev.file233.zombietide.zombie;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.config.ZTSnapshot;
import dev.file233.zombietide.registry.ZTAttachments;
import dev.file233.zombietide.wave.WaveManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Applies the ZombieTide doctrine to any zombie entering the world. Idempotent by design:
 * re-applying is cheap and covers restarts, wave transitions and config edits.
 *
 * <ol>
 *   <li><b>Hygiene</b> — never any armor, hands may hold blocks only, no loot picking,
 *       vanilla door-smashing off (our own block-break system owns that behaviour).</li>
 *   <li><b>Flesh</b> — speed/damage hard caps from the config (1.2× player speed max,
 *       2-heart damage max), escalating follow-range, reinforcement odds, knockback
 *       resistance while a wave rages.</li>
 *   <li><b>Brain</b> — the no-line-of-sight {@link ZTHuntPlayerGoal}, sun goals removed
 *       mid-wave (the horde does not hide in shadows), block-breaking goal for the chosen
 *       from wave 20 onward.</li>
 * </ol>
 */
public final class ZombieMutation {
    private ZombieMutation() {}

    public static void apply(Zombie zombie, boolean loadedFromDisk) {
        if (zombie.level().isClientSide()) return;
        ZTSnapshot snap = ZTSnapshot.get();
        if (!snap.enabled) return;
        int wave = WaveManager.currentWave();
        boolean active = WaveManager.isWaveActive();

        applyGearRules(zombie, snap);
        applyAttributes(zombie, wave, active, snap);
        applyBrain(zombie, wave, active, snap);
    }

    // ------------------------------------------------------------------ gear
    private static void applyGearRules(Zombie zombie, ZTSnapshot snap) {
        if (snap.stripArmor) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !zombie.getItemBySlot(slot).isEmpty()) {
                    zombie.setItemSlot(slot, ItemStack.EMPTY);
                    zombie.setDropChance(slot, 0.0F);
                }
            }
        }
        if (snap.blockItemsOnly) {
            // strip any non-block weapon/tool that rollout or a dungeon granted
            ItemStack main = zombie.getMainHandItem();
            ItemStack off = zombie.getOffhandItem(); // fetched once: each call rebuilds a view otherwise
            if (!main.isEmpty() && !(main.getItem() instanceof BlockItem)) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                main = ItemStack.EMPTY;
            }
            if (!off.isEmpty() && !(off.getItem() instanceof BlockItem)) {
                zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            // the block-carriers of the horde
            var held = ZTConfig.heldBlocks();
            if (main.isEmpty() && !held.isEmpty()
                    && zombie.getRandom().nextFloat() < snap.heldBlockChance) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND,
                        new ItemStack(held.get(zombie.getRandom().nextInt(held.size()))));
                zombie.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
            }
            // a zombie is a claw, not a pack mule: never vacuum up armor from the ground
            zombie.setCanPickUpLoot(false);
        }
        // vanilla door-busting is replaced by ZombieTide's wave-gated block breaking (wave 20+)
        zombie.setCanBreakDoors(false);
    }

    // ------------------------------------------------------------------ flesh
    private static void applyAttributes(Zombie zombie, int wave, boolean waveActive, ZTSnapshot snap) {
        setBase(zombie, Attributes.MOVEMENT_SPEED, snap.speedFor(wave, waveActive));
        setBase(zombie, Attributes.FOLLOW_RANGE, snap.followFor(wave, waveActive));

        // ---- damage: grows with the waves, never breaches the 2-heart law ----
        AttributeInstance damage = zombie.getAttribute(Attributes.ATTACK_DAMAGE);
        double cap = snap.maxDamage;
        if (damage != null) {
            double desired = snap.attackFor(wave);
            if (damage.getBaseValue() > cap) damage.setBaseValue(cap);
            else if (desired > damage.getBaseValue()) damage.setBaseValue(desired);
        }

        // ---- flesh: beefier every wave, but NEVER more than player + 5 hearts ----
        AttributeInstance health = zombie.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            // the victim's real max health is the yardstick. The zombie's own target is a
            // one-field read; only untargeted ghouls take the wider (player-list) fallback.
            double playerRef = 20.0D;
            if (zombie.getTarget() instanceof Player prey) {
                playerRef = prey.getMaxHealth();
            } else {
                Player ref = levelNearestPlayer(zombie);
                if (ref != null) playerRef = ref.getMaxHealth();
            }
            double newMax = snap.healthFor(wave, playerRef);
            double oldMax = health.getBaseValue();
            if (newMax != oldMax) {
                boolean wasHealthy = zombie.getHealth() >= (float) oldMax - 0.01F;
                health.setBaseValue(newMax);
                if (wasHealthy || zombie.getHealth() > zombie.getMaxHealth()) {
                    zombie.setHealth((float) zombie.getMaxHealth());
                }
            }
        }

        setBase(zombie, Attributes.SPAWN_REINFORCEMENTS_CHANCE, snap.reinforcementsFor(wave, waveActive));
        setBase(zombie, Attributes.KNOCKBACK_RESISTANCE, waveActive ? snap.kbrFor(wave) : 0.0D);
    }

    /**
     * Nearest player fallback without vanilla's nearest-entity search: the player list of
     * a dimension is tiny (usually size 1), so walking it directly beats the spatial
     * machinery {@code level.getNearestPlayer} spins up for every mutation.
     */
    @org.jetbrains.annotations.Nullable
    private static Player levelNearestPlayer(Zombie zombie) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player player : zombie.level().players()) {
            double dist = player.distanceToSqr(zombie);
            if (dist < bestDist) { bestDist = dist; best = player; }
        }
        return bestDist <= 64.0D * 64.0D ? best : null;
    }

    private static void setBase(Zombie zombie, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance instance = zombie.getAttribute(attr);
        if (instance != null && instance.getBaseValue() != value) {
            instance.setBaseValue(value);
        }
    }

    // ------------------------------------------------------------------ brain
    private static void applyBrain(Zombie zombie, int wave, boolean waveActive, ZTSnapshot snap) {
        ensureHuntGoal(zombie);

        boolean firstTouch = !zombie.getData(ZTAttachments.WAVE_TAGGED);
        if (firstTouch) {
            zombie.setData(ZTAttachments.WAVE_TAGGED, true);
            // one lifetime roll per zombie: is it one of the wall-chewers?
            if (snap.breacherGateFor(wave)
                    && zombie.getRandom().nextFloat() < snap.blockBreakChance) {
                ensureBlockBreakGoal(zombie);
            }
        }

        // mid-wave the horde ignores the sun entirely
        if (waveActive && snap.noBurnInWaves) {
            removeSunGoals(zombie);
        } else {
            restoreSunGoals(zombie);
        }
    }

    private static void ensureHuntGoal(Zombie zombie) {
        for (WrappedGoal wrapped : zombie.targetSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof ZTHuntPlayerGoal) return;
        }
        // priority just below vanilla player targeting (2), re-evaluated briskly
        zombie.targetSelector.addGoal(2, new ZTHuntPlayerGoal(zombie, 10));
    }

    private static void ensureBlockBreakGoal(Zombie zombie) {
        for (WrappedGoal wrapped : zombie.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof BlockBreakGoal) return;
        }
        zombie.goalSelector.addGoal(4, new BlockBreakGoal(zombie));
    }

    private static void removeSunGoals(Zombie zombie) {
        List<Goal> doomed = new ArrayList<>();
        for (WrappedGoal wrapped : zombie.goalSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof FleeSunGoal || goal instanceof RestrictSunGoal) doomed.add(goal);
        }
        for (Goal goal : doomed) zombie.goalSelector.removeGoal(goal);
    }

    private static void restoreSunGoals(Zombie zombie) {
        // only sun-sensitive zombie types ever owned these goals (plain zombie & drowned)
        EntityType<?> type = zombie.getType();
        if (type != EntityType.ZOMBIE && type != EntityType.DROWNED) return;
        boolean hasRestrict = false, hasFlee = false;
        for (WrappedGoal wrapped : zombie.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof RestrictSunGoal) hasRestrict = true;
            if (wrapped.getGoal() instanceof FleeSunGoal) hasFlee = true;
        }
        if (!hasRestrict) zombie.goalSelector.addGoal(3, new RestrictSunGoal(zombie));
        if (!hasFlee) zombie.goalSelector.addGoal(3, new FleeSunGoal(zombie, 1.0D));
    }

    /** Re-applies the doctrine to every loaded zombie (wave start/end, resets, config surgery). */
    public static void sweep(MinecraftServer server) {
        if (WaveManager.overworldOnly()) {
            sweepLevel(server.overworld()); // default case: no dimension set to walk
            return;
        }
        for (var key : ZTConfig.dimensions()) {
            ServerLevel level = server.getLevel(key);
            if (level != null) sweepLevel(level);
        }
    }

    private static void sweepLevel(ServerLevel level) {
        for (var player : level.players()) {
            for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(160.0D))) {
                apply(zombie, false);
            }
        }
    }
}
