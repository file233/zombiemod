package dev.file233.zombietide.zombie;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.file233.zombietide.config.ZTConfig;
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
        int wave = WaveManager.currentWave();
        boolean active = WaveManager.isWaveActive();

        applyGearRules(zombie);
        applyAttributes(zombie, wave, active);
        applyBrain(zombie, wave, active);
    }

    // ------------------------------------------------------------------ gear
    private static void applyGearRules(Zombie zombie) {
        if (ZTConfig.Z_STRIP_ARMOR.get()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !zombie.getItemBySlot(slot).isEmpty()) {
                    zombie.setItemSlot(slot, ItemStack.EMPTY);
                    zombie.setDropChance(slot, 0.0F);
                }
            }
        }
        if (ZTConfig.Z_BLOCK_ITEMS_ONLY.get()) {
            // strip any non-block weapon/tool that rollout or a dungeon granted
            if (!zombie.getMainHandItem().isEmpty() && !(zombie.getMainHandItem().getItem() instanceof BlockItem)) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            if (!zombie.getOffhandItem().isEmpty() && !(zombie.getOffhandItem().getItem() instanceof BlockItem)) {
                zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            // the block-carriers of the horde
            var held = ZTConfig.heldBlocks();
            if (zombie.getMainHandItem().isEmpty() && !held.isEmpty()
                    && zombie.getRandom().nextFloat() < ZTConfig.Z_HELD_BLOCK_CHANCE.get()) {
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
    private static void applyAttributes(Zombie zombie, int wave, boolean waveActive) {
        setBase(zombie, Attributes.MOVEMENT_SPEED, ZTConfig.zombieSpeed(wave));
        setBase(zombie, Attributes.FOLLOW_RANGE, ZTConfig.followRange(wave, waveActive));

        // ---- damage: grows with the waves, never breaches the 2-heart law ----
        AttributeInstance damage = zombie.getAttribute(Attributes.ATTACK_DAMAGE);
        double cap = ZTConfig.maxZombieDamage();
        if (damage != null) {
            double desired = ZTConfig.zombieAttackDamage(wave);
            if (damage.getBaseValue() > cap) damage.setBaseValue(cap);
            else if (desired > damage.getBaseValue()) damage.setBaseValue(desired);
        }

        // ---- flesh: beefier every wave, but NEVER more than player + 5 hearts ----
        AttributeInstance health = zombie.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            // the victim's real max health is the yardstick (fallback: vanilla 10 hearts)
            double playerRef = 20.0D;
            Player ref = zombie.level().getNearestPlayer(zombie, 64.0D);
            if (ref != null) playerRef = ref.getMaxHealth();
            double newMax = ZTConfig.zombieHealth(wave, playerRef);
            double oldMax = health.getBaseValue();
            if (newMax != oldMax) {
                boolean wasHealthy = zombie.getHealth() >= (float) oldMax - 0.01F;
                health.setBaseValue(newMax);
                if (wasHealthy || zombie.getHealth() > zombie.getMaxHealth()) {
                    zombie.setHealth((float) zombie.getMaxHealth());
                }
            }
        }

        int fromWave = ZTConfig.Z_REINFORCEMENT_FROM_WAVE.get();
        double reinforcements = wave >= fromWave
                ? Math.min(ZTConfig.Z_REINFORCEMENT_CAP.get(), 0.1D + wave * ZTConfig.Z_REINFORCEMENT_PER_WAVE.get())
                : 0.1D; // vanilla default
        setBase(zombie, Attributes.SPAWN_REINFORCEMENTS_CHANCE, reinforcements);

        double kbr = waveActive ? Math.min(0.4D, wave * ZTConfig.Z_KBR_PER_WAVE.get()) : 0.0D;
        setBase(zombie, Attributes.KNOCKBACK_RESISTANCE, kbr);
    }

    private static void setBase(Zombie zombie, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance instance = zombie.getAttribute(attr);
        if (instance != null && instance.getBaseValue() != value) {
            instance.setBaseValue(value);
        }
    }

    // ------------------------------------------------------------------ brain
    private static void applyBrain(Zombie zombie, int wave, boolean waveActive) {
        ensureHuntGoal(zombie);

        boolean firstTouch = !zombie.getData(ZTAttachments.WAVE_TAGGED);
        if (firstTouch) {
            zombie.setData(ZTAttachments.WAVE_TAGGED, true);
            // one lifetime roll per zombie: is it one of the wall-chewers?
            if (wave >= ZTConfig.Z_BLOCK_BREAK_FROM_WAVE.get()
                    && zombie.getRandom().nextFloat() < ZTConfig.Z_BLOCK_BREAK_CHANCE.get()) {
                ensureBlockBreakGoal(zombie);
            }
        }

        // mid-wave the horde ignores the sun entirely
        if (waveActive && ZTConfig.Z_NO_BURN_IN_WAVES.get()) {
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
        for (var key : ZTConfig.dimensions()) {
            ServerLevel level = server.getLevel(key);
            if (level == null) continue;
            for (var player : level.players()) {
                for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(160.0D))) {
                    apply(zombie, false);
                }
            }
        }
    }
}
