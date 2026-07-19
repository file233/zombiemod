package dev.file233.zombietide.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Single source of truth for every tunable in ZombieTide.
 *
 * <p>All values live in a NeoForge COMMON spec, so they are:
 * <ul>
 *   <li>editable via the in-game config screen (Mod List &rarr; ZombieTide &rarr; Config),</li>
 *   <li>editable via {@code /zombietide config set <key> <value>} (persisted to the file),</li>
 *   <li>readable live — systems read through this class every time they need a value.</li>
 * </ul>
 *
 * Numeric balance knobs that the design brief pins down are defaulted exactly to spec:
 * 8-minute first wave, +2 minutes per wave, 50 waves, 2-heart damage cap, 1.2x player speed,
 * block-breaking from wave 20-only, near-zero spawn rates for variants/other hostiles mid-wave.
 */
public final class ZTConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private ZTConfig() {}

    /** What happens after the final configured wave ends. */
    public enum AfterLastWave { CONTINUE, LOOP, STOP }

    // ------------------------------------------------------------------ spec
    public static final ModConfigSpec SPEC;

    // general
    public static final ModConfigSpec.BooleanValue ENABLED;

    // waves
    public static final ModConfigSpec.IntValue MAX_WAVES;
    public static final ModConfigSpec.DoubleValue FIRST_WAVE_MINUTES;
    public static final ModConfigSpec.DoubleValue WAVE_INCREMENT_MINUTES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> INTERVAL_OVERRIDES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DURATION_OVERRIDES;
    public static final ModConfigSpec.DoubleValue ALARM_SECONDS;
    public static final ModConfigSpec.ConfigValue<String> ALARM_SOUND;
    public static final ModConfigSpec.DoubleValue ALARM_VOLUME;
    public static final ModConfigSpec.DoubleValue ALARM_PITCH;
    public static final ModConfigSpec.ConfigValue<AfterLastWave> AFTER_LAST_WAVE;
    public static final ModConfigSpec.BooleanValue CHAT_ANNOUNCE;
    public static final ModConfigSpec.BooleanValue TITLE_ANNOUNCE;

    // zombies
    public static final ModConfigSpec.DoubleValue Z_BASE_SPEED;
    public static final ModConfigSpec.DoubleValue Z_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue Z_SPEED_PER_WAVE;
    public static final ModConfigSpec.DoubleValue Z_FOLLOW_RANGE;
    public static final ModConfigSpec.DoubleValue Z_FOLLOW_PER_WAVE;
    public static final ModConfigSpec.DoubleValue Z_FOLLOW_CAP;
    public static final ModConfigSpec.DoubleValue Z_FOLLOW_WAVE_BONUS;
    public static final ModConfigSpec.BooleanValue Z_HUNT_WITHOUT_SIGHT;
    public static final ModConfigSpec.DoubleValue Z_MAX_DAMAGE_HEARTS;
    public static final ModConfigSpec.DoubleValue Z_DAMAGE_PER_WAVE_HEARTS;
    public static final ModConfigSpec.DoubleValue Z_HEALTH_BASE_HEARTS;
    public static final ModConfigSpec.DoubleValue Z_HEALTH_PER_WAVE_HEARTS;
    public static final ModConfigSpec.DoubleValue Z_HEALTH_MAX_ABOVE_PLAYER;
    public static final ModConfigSpec.DoubleValue Z_KBR_PER_WAVE;
    public static final ModConfigSpec.IntValue Z_REINFORCEMENT_FROM_WAVE;
    public static final ModConfigSpec.DoubleValue Z_REINFORCEMENT_PER_WAVE;
    public static final ModConfigSpec.DoubleValue Z_REINFORCEMENT_CAP;
    public static final ModConfigSpec.DoubleValue Z_HEARING_RADIUS;
    public static final ModConfigSpec.DoubleValue Z_HEARING_PER_WAVE;
    public static final ModConfigSpec.DoubleValue Z_HEARING_CAP;
    public static final ModConfigSpec.IntValue Z_NOISE_COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue Z_STRIP_ARMOR;
    public static final ModConfigSpec.BooleanValue Z_BLOCK_ITEMS_ONLY;
    public static final ModConfigSpec.DoubleValue Z_HELD_BLOCK_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> Z_HELD_BLOCKS;
    public static final ModConfigSpec.IntValue Z_BLOCK_BREAK_FROM_WAVE;
    public static final ModConfigSpec.DoubleValue Z_BLOCK_BREAK_CHANCE;
    public static final ModConfigSpec.DoubleValue Z_BLOCK_BREAK_MAX_HARDNESS;
    public static final ModConfigSpec.IntValue Z_BLOCK_BREAK_COOLDOWN;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> Z_BLOCK_BREAK_BLACKLIST;
    public static final ModConfigSpec.BooleanValue Z_BLOCK_BREAK_ONLY_WAVES;
    public static final ModConfigSpec.BooleanValue Z_NO_BURN_IN_WAVES;
    public static final ModConfigSpec.DoubleValue Z_BABY_KEEP_CHANCE;
    public static final ModConfigSpec.DoubleValue Z_VARIANT_KEEP_CHANCE;
    public static final ModConfigSpec.DoubleValue Z_NON_ZOMBIE_KEEP_CHANCE;
    public static final ModConfigSpec.BooleanValue Z_CONVERT_VARIANTS;

    // targeting
    public static final ModConfigSpec.BooleanValue T_CREATIVE;
    public static final ModConfigSpec.BooleanValue T_SPECTATORS;

    // intelligence (the brains dial)
    public static final ModConfigSpec.DoubleValue I_BASE;
    public static final ModConfigSpec.DoubleValue I_PER_WAVE;
    public static final ModConfigSpec.DoubleValue I_MAX;
    public static final ModConfigSpec.IntValue I_UNSEEN_MEMORY;
    public static final ModConfigSpec.IntValue I_UNSEEN_PER_WAVE;

    // frenzy (the wave-rage dial)
    public static final ModConfigSpec.DoubleValue F_INTENSITY;
    public static final ModConfigSpec.DoubleValue F_SPEED_BOOST;
    public static final ModConfigSpec.DoubleValue F_HEARING_BONUS;
    public static final ModConfigSpec.DoubleValue F_NOISE_FACTOR;

    // spawning
    public static final ModConfigSpec.BooleanValue S_ENABLED;
    public static final ModConfigSpec.IntValue S_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue S_ATTEMPTS_PER_CYCLE;
    public static final ModConfigSpec.IntValue S_RING_MIN;
    public static final ModConfigSpec.IntValue S_RING_MAX;
    public static final ModConfigSpec.IntValue S_CAP_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue S_CAP_PER_WAVE;
    public static final ModConfigSpec.IntValue S_CAP_MAX;
    public static final ModConfigSpec.BooleanValue S_DAYLIGHT_SPAWN;
    public static final ModConfigSpec.BooleanValue S_BOOST_NATURAL_PLACEMENT;
    public static final ModConfigSpec.DoubleValue S_SURFACE_CHANCE;
    public static final ModConfigSpec.BooleanValue S_IGNORE_GAMERULE;
    public static final ModConfigSpec.BooleanValue S_PRESSURE_CREATIVE;
    public static final ModConfigSpec.DoubleValue S_DAY_SPAWN_FACTOR;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> S_DIMENSIONS;

    // effects
    public static final ModConfigSpec.DoubleValue E_PROC_CHANCE;
    public static final ModConfigSpec.BooleanValue E_ONLY_DURING_WAVES;
    public static final ModConfigSpec.IntValue E_AMPLIFIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> E_LIST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.translation("zombietide.configuration.general").push("general");
        ENABLED = b.comment(
                "Master switch. When false the mod is completely inert.",
                "Công tắc tổng của mod.")
                .translation("zombietide.configuration.enabled")
                .define("enabled", true);
        b.pop();

        // ------------------------------------------------------------------ waves
        b.translation("zombietide.configuration.waves").push("waves");
        MAX_WAVES = b.comment("Total number of waves. Default 50, exactly as designed.",
                "Tổng số đợt tấn công (mặc định 50).")
                .translation("zombietide.configuration.maxWaves")
                .defineInRange("maxWaves", 50, 1, 100000);
        FIRST_WAVE_MINUTES = b.comment("Duration of wave #1, in minutes. Default 8.",
                "Thởi gian (phút) của đợt đầu tiên. Mặc định 8.")
                .translation("zombietide.configuration.firstWaveMinutes")
                .defineInRange("firstWaveMinutes", 8.0D, 0.005D, 24000.0D);
        WAVE_INCREMENT_MINUTES = b.comment("Each wave lasts this many minutes longer than the previous. Default 2.",
                "Mỗi đợt sau dài hơn đợt trước bấy nhiêu phút. Mặc định 2.")
                .translation("zombietide.configuration.waveIncrementMinutes")
                .defineInRange("waveIncrementMinutes", 2.0D, 0.0D, 12000.0D);
        INTERVAL_OVERRIDES = b.comment("Per-wave calm-gap overrides, format: wave=seconds (e.g. \"5=900\", \"10=300\").",
                "These exact values beat the formula for the rest before that specific wave.",
                "Editable live via /zombietide interval <wave> <seconds|clear>.",
                "Chỉnh khoảng nghỉ RIÊNG cho từng đợt, dạng đợt=giây (vd \"5=900\").")
                .translation("zombietide.configuration.intervalOverrides")
                .defineListAllowEmpty("intervalOverrides", List::of, o -> o instanceof String s && s.matches("\\d+\\s*[=:]\\s*\\d+"));
        DURATION_OVERRIDES = b.comment("Per-wave DURATION overrides, format: wave=seconds (e.g. \"20=1800\").",
                "These exact values beat the firstWave/+perWave formula for that specific wave.",
                "Editable live via /zombietide duration <wave> <seconds|clear>.",
                "Chỉnh thởi lượng RIÊNG cho từng đợt, dạng đợt=giây (vd \"20=1800\").")
                .translation("zombietide.configuration.durationOverrides")
                .defineListAllowEmpty("durationOverrides", List::of, o -> o instanceof String s && s.matches("\\d+\\s*[=:]\\s*\\d+"));
        ALARM_SECONDS = b.comment("Seconds of alarm sirene before a wave begins. Default 5.",
                "Số giây còi báo động trước khi đợt bắt đầu. Mặc định 5.")
                .translation("zombietide.configuration.alarmSeconds")
                .defineInRange("alarmSeconds", 5.0D, 0.01D, 3000.0D);
        ALARM_SOUND = b.comment("Sound event id played as the wave alarm.",
                "Âm thanh báo động.")
                .translation("zombietide.configuration.alarmSound")
                .define("alarmSound", "zombietide:wave_alarm");
        ALARM_VOLUME = b.comment("Alarm volume (also controls how far it can be heard).",
                "Âm lượng báo động.")
                .translation("zombietide.configuration.alarmVolume")
                .defineInRange("alarmVolume", 1.0D, 0.001D, 400.0D);
        ALARM_PITCH = b.comment("Alarm pitch (1.0 = the sirene as recorded).",
                "Cao độ của còi báo động (1.0 = nguyên bản).")
                .translation("zombietide.configuration.alarmPitch")
                .defineInRange("alarmPitch", 1.0D, 0.005D, 200.0D);
        AFTER_LAST_WAVE = b.comment("After the final wave: CONTINUE = endless max-intensity waves, LOOP = restart from wave 1, STOP = the apocalypse is survived.",
                "Sau đợt cuối: CONTINUE (lặp đợt cuối vô hạn) / LOOP (quay lại đợt 1) / STOP (dừng hẳn).")
                .translation("zombietide.configuration.afterLastWave")
                .defineEnum("afterLastWave", AfterLastWave.CONTINUE);
        CHAT_ANNOUNCE = b.comment("Announce wave start/end in chat.",
                "Thông báo đợt trong khung chat.").translation("zombietide.configuration.chatAnnounce").define("chatAnnounce", true);
        TITLE_ANNOUNCE = b.comment("Show a big title on wave start/end.",
                "Hiện tiêu đề lớn khi đợt bắt đầu/kết thúc.").translation("zombietide.configuration.titleAnnounce").define("titleAnnounce", true);
        b.pop();

        // ------------------------------------------------------------------ zombies
        b.translation("zombietide.configuration.zombies").push("zombies");
        Z_BASE_SPEED = b.comment("Baseline movement-speed attribute of wave zombies. Player walking = 0.1.",
                "Tốc độ cơ bản của zombie (0.1 = tốc độ đi bộ của ngưởi chơi).")
                .translation("zombietide.configuration.baseSpeed")
                .defineInRange("baseSpeed", 0.10D, 0.0001D, 100.0D);
        Z_MAX_SPEED = b.comment("HARD CAP for zombie speed: 0.20 is exactly 2x the player base speed.",
                "Trần tốc độ tuyệt đối của zombie: 0.20 = GẤP 2 LẦN tốc độ ngưởi chơi.")
                .translation("zombietide.configuration.maxSpeed")
                .defineInRange("maxSpeed", 0.20D, 0.0001D, 100.0D);
        Z_SPEED_PER_WAVE = b.comment("Speed gained per completed wave (never exceeds maxSpeed).",
                "Tốc độ tăng thêm sau mỗi đợt (không vượt quá maxSpeed).")
                .translation("zombietide.configuration.speedPerWave")
                .defineInRange("speedPerWave", 0.0008D, 0.0D, 5.0D);
        Z_FOLLOW_RANGE = b.comment("Baseline target detection range (blocks).",
                "Tầm phát hiện mục tiêu cơ bản (khối).")
                .translation("zombietide.configuration.followRange")
                .defineInRange("followRange", 40.0D, 0.08D, 51200.0D);
        Z_FOLLOW_PER_WAVE = b.comment("Target range gained per wave.",
                "Tầm phát hiện tăng mỗi đợt.")
                .translation("zombietide.configuration.followRangePerWave")
                .defineInRange("followRangePerWave", 1.5D, 0.0D, 3200.0D);
        Z_FOLLOW_CAP = b.comment("Target range hard cap.",
                "Trần tầm phát hiện.")
                .translation("zombietide.configuration.followRangeCap")
                .defineInRange("followRangeCap", 128.0D, 0.16D, 51200.0D);
        Z_FOLLOW_WAVE_BONUS = b.comment("Extra target range while a wave is active.",
                "Tầm phát hiện cộng thêm khi đợt đang diễn ra.")
                .translation("zombietide.configuration.waveFollowBonus")
                .defineInRange("waveFollowBonus", 16.0D, 0.0D, 12800.0D);
        Z_HUNT_WITHOUT_SIGHT = b.comment("Zombies acquire players even without line of sight (hearing/smell).",
                "Zombie đuổi theo kể cả khi không nhìn thấy (bằng mũỉ + tai).")
                .translation("zombietide.configuration.huntWithoutSight")
                .define("huntWithoutSight", true);
        Z_MAX_DAMAGE_HEARTS = b.comment("HARD CAP of zombie melee damage against players, in HEARTS. Default 2 hearts.",
                "Sát thương tối đa của zombie lên ngưởi chơi, tính theo TIM. Mặc định 2 tim.")
                .translation("zombietide.configuration.maxDamageHearts")
                .defineInRange("maxDamageHearts", 2.0D, 0.005D, 2000.0D);
        Z_DAMAGE_PER_WAVE_HEARTS = b.comment("Damage growth per wave, in HEARTS (still clamped by maxDamageHearts). Default 0 = flat.",
                "Sát thương tăng thêm mỗi đợt (TIM), vẫn không vượt trần maxDamageHearts.")
                .translation("zombietide.configuration.damagePerWaveHearts")
                .defineInRange("damagePerWaveHearts", 0.0D, 0.0D, 200.0D);
        Z_HEALTH_BASE_HEARTS = b.comment("Zombie health at wave 0, in HEARTS (vanilla = 10 hearts).",
                "Máu zombie lúc đầu, tính theo TIM (vanilla = 10 tim).")
                .translation("zombietide.configuration.healthBaseHearts")
                .defineInRange("healthBaseHearts", 10.0D, 0.01D, 20000.0D);
        Z_HEALTH_PER_WAVE_HEARTS = b.comment("Health growth per wave, in HEARTS (0.25 = +1 heart every 4 waves).",
                "Máu tăng thêm mỗi đợt (TIM).")
                .translation("zombietide.configuration.healthPerWaveHearts")
                .defineInRange("healthPerWaveHearts", 0.25D, 0.0D, 1000.0D);
        Z_HEALTH_MAX_ABOVE_PLAYER = b.comment("HARD CAP: a zombie may have AT MOST this many HEARTS above the nearest player's max health.",
                "Default 5 hearts → with a vanilla 10-heart player the beefiest zombie has 15 hearts.",
                "Trần máu zombie = máu ngưởi chơi + giá trị này (TIM). Mặc định 5 tim → zombie trâu nhất 15 tim.")
                .translation("zombietide.configuration.healthMaxHeartsAbovePlayer")
                .defineInRange("healthMaxHeartsAbovePlayer", 5.0D, 0.0D, 10000.0D);
        Z_KBR_PER_WAVE = b.comment("Knockback resistance gained per wave (during waves).",
                "Kháng knockback tăng theo đợt.")
                .translation("zombietide.configuration.knockbackResistancePerWave")
                .defineInRange("knockbackResistancePerWave", 0.02D, 0.0D, 10.0D);
        Z_REINFORCEMENT_FROM_WAVE = b.comment("From this wave on, hurt zombies can call reinforcements.",
                "Từ đợt này zombie bị đánh có thể gọi bạn.")
                .translation("zombietide.configuration.reinforcementFromWave")
                .defineInRange("reinforcementFromWave", 6, 1, 100000);
        Z_REINFORCEMENT_PER_WAVE = b.comment("Reinforcement chance gained per wave.",
                "Tỉ lệ gọi bạn tăng mỗi đợt.")
                .translation("zombietide.configuration.reinforcementPerWave")
                .defineInRange("reinforcementPerWave", 0.005D, 0.0D, 20.0D);
        Z_REINFORCEMENT_CAP = b.comment("Reinforcement chance cap.",
                "Trần tỉ lệ gọi bạn.")
                .translation("zombietide.configuration.reinforcementCap")
                .defineInRange("reinforcementCap", 0.35D, 0.0D, 100.0D);
        Z_HEARING_RADIUS = b.comment("Radius (blocks) in which zombies hear noises you make.",
                "Bán kính zombie nghe được tiếng động.").translation("zombietide.configuration.hearingRadius").defineInRange("hearingRadius", 24.0D, 0.04D, 25600.0D);
        Z_HEARING_PER_WAVE = b.comment("Hearing radius gained per wave.",
                "Bán kính nghe tăng mỗi đợt.").translation("zombietide.configuration.hearingPerWave").defineInRange("hearingPerWave", 1.0D, 0.0D, 1600.0D);
        Z_HEARING_CAP = b.comment("Hearing radius cap.",
                "Trần bán kính nghe.").translation("zombietide.configuration.hearingCap").defineInRange("hearingCap", 96.0D, 0.08D, 25600.0D);
        Z_NOISE_COOLDOWN_TICKS = b.comment("Per-zombie re-trigger cooldown for sound alerts.",
                "Thởi gian hồi phản ứng tiếng động của mỗi zombie.")
                .translation("zombietide.configuration.noiseCooldownTicks")
                .defineInRange("noiseCooldownTicks", 35, 5, 60000);
        Z_STRIP_ARMOR = b.comment("Zombies never wear armor (forced every spawn).",
                "Zombie không bao giờ mặc giáp.").translation("zombietide.configuration.stripArmor").define("stripArmor", true);
        Z_BLOCK_ITEMS_ONLY = b.comment("Zombies may only hold BLOCK items (found held weapons are removed).",
                "Zombie chỉ được cầm KHỐI (vũ khí sẽ bị tước).").translation("zombietide.configuration.allowOnlyBlockItems").define("allowOnlyBlockItems", true);
        Z_HELD_BLOCK_CHANCE = b.comment("Chance a zombie spawns holding a random block.",
                "Tỉ lệ zombie sinh ra cầm khối.").translation("zombietide.configuration.heldBlockChance").defineInRange("heldBlockChance", 0.35D, 0.0D, 100.0D);
        Z_HELD_BLOCKS = b.comment("Blocks zombies can be holding.",
                "Danh sách khối zombie có thể cầm.").translation("zombietide.configuration.heldBlocks").defineListAllowEmpty("heldBlocks",
                () -> List.of("minecraft:dirt", "minecraft:oak_planks", "minecraft:cobblestone",
                        "minecraft:glass", "minecraft:oak_log", "minecraft:sand", "minecraft:gravel",
                        "minecraft:bricks", "minecraft:glass_pane", "minecraft:oak_door"),
                o -> o instanceof String s && ResourceLocation.tryParse(s) != null);
        Z_BLOCK_BREAK_FROM_WAVE = b.comment("From this wave on some zombies break blocks. Default 20.",
                "Từ đợt này một số zombie có thể phá khối. Mặc định 20.")
                .translation("zombietide.configuration.blockBreakFromWave")
                .defineInRange("blockBreakFromWave", 20, 1, 100000);
        Z_BLOCK_BREAK_CHANCE = b.comment("Chance any given zombie gets the block-breaking behaviour.",
                "Tỉ lệ zombie có khả năng phá khối.").translation("zombietide.configuration.blockBreakChance").defineInRange("blockBreakChance", 0.35D, 0.0D, 100.0D);
        Z_BLOCK_BREAK_MAX_HARDNESS = b.comment("Maximum block hardness zombies can chew through.",
                "Độ cứng tối đa zombie có thể phá.").translation("zombietide.configuration.blockBreakMaxHardness").defineInRange("blockBreakMaxHardness", 4.0D, 0.001D, 2000.0D);
        Z_BLOCK_BREAK_COOLDOWN = b.comment("Ticks a zombie waits after breaking a block.",
                "Thởi gian nghỉ giữa hai lần phá khối.").translation("zombietide.configuration.blockBreakCooldownTicks").defineInRange("blockBreakCooldownTicks", 30, 5, 60000);
        Z_BLOCK_BREAK_BLACKLIST = b.comment("Blocks zombies will never break.",
                "Khối zombie không bao giờ phá.").translation("zombietide.configuration.blockBreakBlacklist").defineListAllowEmpty("blockBreakBlacklist",
                () -> List.of("minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:reinforced_deepslate",
                        "minecraft:bedrock", "minecraft:end_portal_frame", "minecraft:barrier", "minecraft:chest"),
                o -> o instanceof String s && ResourceLocation.tryParse(s) != null);
        Z_BLOCK_BREAK_ONLY_WAVES = b.comment("If true, block breaking only happens while a wave runs.",
                "Chỉ phá khối khi đang trong đợt.").translation("zombietide.configuration.blockBreakOnlyDuringWaves").define("blockBreakOnlyDuringWaves", false);
        Z_NO_BURN_IN_WAVES = b.comment("Zombies do not burn in sunlight while a wave is active.",
                "Zombie không bị nắng thiêu đốt khi đợt đang diễn ra.").translation("zombietide.configuration.noBurningDuringWaves").define("noBurningDuringWaves", true);
        Z_BABY_KEEP_CHANCE = b.comment("During waves: chance a baby zombie is allowed to exist (else converted to adult).",
                "Tỉ lệ zombie con được giữ lại trong đợt.").translation("zombietide.configuration.babyKeepChance").defineInRange("babyKeepChance", 0.02D, 0.0D, 100.0D);
        Z_VARIANT_KEEP_CHANCE = b.comment("During waves: chance drowned/husk/zombie-villager spawns are kept (else replaced by a plain zombie).",
                "Tỉ lệ biến thể zombie (nước/sa mạc/làng) được giữ lại trong đợt.").translation("zombietide.configuration.variantKeepChance").defineInRange("variantKeepChance", 0.02D, 0.0D, 100.0D);
        Z_NON_ZOMBIE_KEEP_CHANCE = b.comment("During waves: chance any OTHER hostile mob may spawn (skeletons, creepers...).",
                "Tỉ lệ quái gây hại khác được sinh ra trong đợt.").translation("zombietide.configuration.nonZombieKeepChance").defineInRange("nonZombieKeepChance", 0.02D, 0.0D, 100.0D);
        Z_CONVERT_VARIANTS = b.comment("Convert drowned/husk/zombie-villager into plain zombies while a wave runs.",
                "Biến mọi biến thể thành zombie thường khi trong đợt.").translation("zombietide.configuration.convertVariants").define("convertVariants", true);
        b.pop();

        // ------------------------------------------------------------------ targeting
        b.translation("zombietide.configuration.targeting").push("targeting");
        T_CREATIVE = b.comment("Zombies track and chase players in CREATIVE mode too (they still cannot hurt them).",
                "Even builders get stalked by the horde.",
                "Zombie vẫn bám theo ngưởi chơi ở chế độ SÁNG TẠO (dù không gây sát thương được).")
                .translation("zombietide.configuration.targetCreativePlayers")
                .define("targetCreativePlayers", true);
        T_SPECTATORS = b.comment("Zombies may also home in on players in SPECTATOR mode (spooky).",
                "Zombie còn bám cả ngưởi chơi ở chế độ THEO DÕI (spectator).")
                .translation("zombietide.configuration.targetSpectators")
                .define("targetSpectators", false);
        b.pop();

        // ------------------------------------------------------------------ intelligence
        b.translation("zombietide.configuration.intelligence").push("intelligence");
        I_BASE = b.comment("Baseline intelligence level of zombies (1 = trained ghoul).",
                "Affects: detection & hearing reach, reaction latency to noises, target memory.",
                "Độ thông minh cơ bản của zombie (1 = quái đã qua huấn luyện).")
                .translation("zombietide.configuration.intelligenceBase")
                .defineInRange("baseLevel", 1.0D, 0.0D, 1000.0D);
        I_PER_WAVE = b.comment("Intelligence gained per wave (they study you every round).",
                "Độ thông minh tăng sau mỗi đợt (chúng học bạn qua từng trận).")
                .translation("zombietide.configuration.intelligencePerWave")
                .defineInRange("perWaveBonus", 0.04D, 0.0D, 100.0D);
        I_MAX = b.comment("Intelligence cap.",
                "Trần độ thông minh.")
                .translation("zombietide.configuration.intelligenceMax")
                .defineInRange("maxLevel", 10.0D, 0.01D, 10000.0D);
        I_UNSEEN_MEMORY = b.comment("Base ticks a zombie remembers its prey without seeing it.",
                "Số tick cơ bản zombie nhớ con mồi dù không nhìn thấy.")
                .translation("zombietide.configuration.unseenMemoryTicks")
                .defineInRange("unseenMemoryTicks", 60, 10, 60000);
        I_UNSEEN_PER_WAVE = b.comment("Extra memory ticks gained per wave.",
                "Trí nhớ tăng thêm mỗi đợt (tick).")
                .translation("zombietide.configuration.unseenMemoryPerWave")
                .defineInRange("unseenMemoryPerWave", 2, 0, 6000);
        b.pop();

        // ------------------------------------------------------------------ frenzy
        b.translation("zombietide.configuration.frenzy").push("frenzy");
        F_INTENSITY = b.comment("How rabid zombies become DURING waves (scales all wave-only boosts).",
                "0 = zen monks, 1 = the designed blood rage, 3 = absolutely feral.",
                "Mức điên cuồng KHI TRONG ĐỢT (nhân mọi buff chỉ-có-trong-đợt).")
                .translation("zombietide.configuration.frenzyIntensity")
                .defineInRange("intensity", 1.0D, 0.0D, 300.0D);
        F_SPEED_BOOST = b.comment("Movement speed added while a wave is active (never above zombies.maxSpeed).",
                "Tốc độ cộng thêm khi đang trong đợt (không bao giờ vượt zombies.maxSpeed).")
                .translation("zombietide.configuration.frenzySpeedBoost")
                .defineInRange("speedBoost", 0.005D, 0.0D, 5.0D);
        F_HEARING_BONUS = b.comment("Extra hearing radius (blocks) while a wave is active.",
                "Bán kính nghe cộng thêm khi trong đợt.")
                .translation("zombietide.configuration.frenzyHearingBonus")
                .defineInRange("hearingBonus", 8.0D, 0.0D, 6400.0D);
        F_NOISE_FACTOR = b.comment("Reaction-latency multiplier to noises during waves (lower = twitchier).",
                "Hệ số độ trễ phản ứng tiếng động khi trong đợt (nhỏ = nhạy hơn).")
                .translation("zombietide.configuration.frenzyNoiseFactor")
                .defineInRange("noiseReactionFactor", 0.75D, 0.001D, 100.0D);
        b.pop();

        // ------------------------------------------------------------------ spawning
        b.translation("zombietide.configuration.spawning").push("spawning");
        S_ENABLED = b.comment("Enable the wave spawn engine (keeps horde pressure up, even by day).",
                "Bật động cơ sinh zombie theo đợt.").translation("zombietide.configuration.enabled").define("enabled", true);
        S_INTERVAL_TICKS = b.comment("Ticks between spawn cycles per player (20 = 1s).",
                "Tick giữa hai chu kỳ sinh (20 = 1 giây).").translation("zombietide.configuration.intervalTicks").defineInRange("intervalTicks", 40, 5, 120000);
        S_ATTEMPTS_PER_CYCLE = b.comment("Spawn position attempts per cycle.",
                "Số lần thử sinh mỗi chu kỳ.").translation("zombietide.configuration.attemptsPerCycle").defineInRange("attemptsPerCycle", 6, 1, 6400);
        S_RING_MIN = b.comment("Minimum distance from the player for engine spawns.",
                "Khoảng cách tối thiểu tới ngưởi chơi.").translation("zombietide.configuration.ringMinDistance").defineInRange("ringMinDistance", 24, 8, 9600);
        S_RING_MAX = b.comment("Maximum distance from the player for engine spawns.",
                "Khoảng cách tối đa tới ngưởi chơi.").translation("zombietide.configuration.ringMaxDistance").defineInRange("ringMaxDistance", 48, 16, 12800);
        S_CAP_PER_PLAYER = b.comment("Baseline alive-zombie cap per player during waves.",
                "Trần zombie sống quanh mỗi ngưởi chơi.").translation("zombietide.configuration.capPerPlayer").defineInRange("capPerPlayer", 12, 1, 25600);
        S_CAP_PER_WAVE = b.comment("Cap growth per wave.",
                "Trần tăng thêm mỗi đợt.").translation("zombietide.configuration.capPerPlayerPerWave").defineInRange("capPerPlayerPerWave", 0.5D, 0.0D, 1600.0D);
        S_CAP_MAX = b.comment("Absolute alive-zombie cap per player.",
                "Trần tuyệt đối.").translation("zombietide.configuration.capMax").defineInRange("capMax", 48, 1, 51200);
        S_DAYLIGHT_SPAWN = b.comment("Zombies may spawn in full daylight while a wave runs.",
                "Cho phép zombie sinh ban ngày khi đang trong đợt.").translation("zombietide.configuration.daylightSpawn").define("daylightSpawn", true);
        S_BOOST_NATURAL_PLACEMENT = b.comment("Also force-allow vanilla natural zombie spawns (ignores light) during waves.",
                "Luôn cho phép cơ chế sinh tự nhiên bỏ qua ánh sáng khi trong đợt.").translation("zombietide.configuration.boostNaturalPlacement").define("boostNaturalPlacement", true);
        S_SURFACE_CHANCE = b.comment("Chance a spawn attempt targets the surface (else caves around the player).",
                "Tỉ lệ thử sinh trên mặt đất.").translation("zombietide.configuration.surfaceChance").defineInRange("surfaceChance", 0.6D, 0.0D, 100.0D);
        S_IGNORE_GAMERULE = b.comment("Spawn even when the doMobSpawning gamerule is false.",
                "Bỏ qua gamerule doMobSpawning.").translation("zombietide.configuration.ignoreDoMobSpawningRule").define("ignoreDoMobSpawningRule", false);
        S_PRESSURE_CREATIVE = b.comment("Also besiege players in creative mode during waves (spawner treats them as anchors).",
                "Khi trong đợt, zombie vẫn xuất hiện vây quanh ngưởi chơi ở chế độ sáng tạo.")
                .translation("zombietide.configuration.pressureCreativePlayers")
                .define("pressureCreativePlayers", true);
        S_DAY_SPAWN_FACTOR = b.comment("Wave-spawn rate while the sun is up, relative to night (0.5 = HALF as many by day).",
                "The tide still comes at noon — just thinner light-shy ranks.",
                "Tỉ lệ sinh zombie ban ngày so với ban đêm (0.5 = ÍT HƯN 2 LẦN).")
                .translation("zombietide.configuration.daySpawnFactor")
                .defineInRange("daySpawnFactor", 0.5D, 0.0D, 100.0D);
        S_DIMENSIONS = b.comment("Dimensions where waves and the spawn engine apply.",
                "Các chiều (dimension) áp dụng hệ thống đợt.").translation("zombietide.configuration.dimensions").defineListAllowEmpty("dimensions",
                () -> List.of("minecraft:overworld"), o -> o instanceof String s && ResourceLocation.tryParse(s) != null);
        b.pop();

        // ------------------------------------------------------------------ effects
        b.translation("zombietide.configuration.effects").push("effects");
        E_PROC_CHANCE = b.comment("Chance a zombie hit applies a random harmful effect.",
                "Tỉ lệ nhận hiệu ứng xấu khi bị zombie đánh.").translation("zombietide.configuration.procChance").defineInRange("procChance", 0.35D, 0.0D, 100.0D);
        E_ONLY_DURING_WAVES = b.comment("Apply harmful effects only while a wave is running.",
                "Chỉ gây hiệu ứng khi trong đợt.").translation("zombietide.configuration.onlyDuringWaves").define("onlyDuringWaves", false);
        E_AMPLIFIER = b.comment("Amplifier of applied effects (0 = level I, 1 = level II, ...).",
                "Cấp độ hiệu ứng (0 = cấp I, 1 = cấp II).").translation("zombietide.configuration.amplifier").defineInRange("amplifier", 0, 0, 300);
        E_LIST = b.comment(
                "Effect pool. Format: effect_id|min_seconds|max_seconds|min_wave|weight",
                "id: minecraft vanilla effect (slowness, weakness, poison, blindness, nausea, ...),",
                "min_wave: earliest wave this effect can appear in; weight: relative probability.")
                .translation("zombietide.configuration.list")
                .defineListAllowEmpty("list", () -> List.of(
                        "minecraft:slowness|4|8|1|40",
                        "minecraft:weakness|4|10|1|30",
                        "minecraft:mining_fatigue|5|10|3|25",
                        "minecraft:nausea|4|8|5|20",
                        "minecraft:blindness|3|6|8|18",
                        "minecraft:poison|3|6|10|14",
                        "minecraft:hunger|5|12|12|12",
                        "minecraft:wither|2|4|30|6",
                        "minecraft:darkness|4|8|40|5"),
                        o -> o instanceof String);
        b.pop();

        SPEC = b.build();
    }

    // ------------------------------------------------------------------ live helpers
    public static boolean enabled() { return ENABLED.get(); }

    /** Wave duration in ticks for the given wave number (1-based). Per-wave override wins. */
    public static long waveDurationTicks(int wave) {
        Integer override = durationOverrides().get(wave);
        if (override != null) return Math.max(100L, override * 20L);
        double minutes = FIRST_WAVE_MINUTES.get() + Math.max(0, wave - 1) * WAVE_INCREMENT_MINUTES.get();
        return Math.max(100L, Math.round(minutes * 1200.0D));
    }

    /** Calm gap default (seconds) when a wave has no explicit per-wave override. */
    public static final int DEFAULT_CALM_SECONDS = 600;

    /** Default calm gap in seconds — there is no global knob anymore, only per-wave editing. */
    public static int defaultCalmSeconds() { return DEFAULT_CALM_SECONDS; }

    /** Calm gap in ticks before the given wave number arrives: its own override, else the default. */
    public static long calmTicks(int nextWave) {
        Integer override = intervalOverrides().get(nextWave);
        long seconds = override != null ? override : DEFAULT_CALM_SECONDS;
        return Math.max(100L, seconds * 20L);
    }

    /** How the calm gap before {@code wave} is computed: "override" or "default". */
    public static String calmSource(int wave) {
        return intervalOverrides().containsKey(wave) ? "override" : "default";
    }

    /**
     * Wave number normalized to the 50-wave design baseline. Every growth formula runs on
     * THIS value, so whether you configure 10, 50 or 500 waves, the FINAL wave is always
     * exactly as hard as the designed wave 50 — the apocalypse always peaks at the end.
     */
    public static double designWave(int wave) {
        return wave * (50.0D / Math.max(1, MAX_WAVES.get()));
    }

    /** How the duration of {@code wave} is computed: "override" or "formula". */
    public static String durationSource(int wave) {
        return durationOverrides().containsKey(wave) ? "override" : "formula";
    }

    /** Zombie hunting whitelist for players (creative/spectator are opt-in via config). */
    public static boolean isHuntable(net.minecraft.world.entity.player.Player player) {
        if (!player.isAlive()) return false;
        if (player.isSpectator()) return T_SPECTATORS.get();
        if (player.isCreative()) return T_CREATIVE.get();
        return true;
    }

    /**
     * Zombie max health for a wave, in health points. Grows linearly with the wave count,
     * but is hard-capped at {@code playerMaxHealth + healthMaxHeartsAbovePlayer} hearts —
     * by design a zombie may only ever be 5 hearts beefier than its victim.
     */
    public static double zombieHealth(int wave, double playerMaxHealth) {
        double built = (Z_HEALTH_BASE_HEARTS.get() + Math.max(0, designWave(wave)) * Z_HEALTH_PER_WAVE_HEARTS.get()) * 2.0D;
        double cap = Math.max(2.0D, playerMaxHealth) + Z_HEALTH_MAX_ABOVE_PLAYER.get() * 2.0D;
        return Math.max(2.0D, Math.min(built, cap));
    }

    /** Zombie attack damage attribute target for a wave (still below maxZombieDamage). */
    public static double zombieAttackDamage(int wave) {
        return Math.min(maxZombieDamage(), 3.0D + Math.max(0, designWave(wave)) * Z_DAMAGE_PER_WAVE_HEARTS.get() * 2.0D);
    }

    /**
     * Alarm lead time in ticks.
     * <p><b>Hot-path note:</b> read of {@code ALARM_SECONDS} is one map hit — fine once per
     * tick by the wave conductor, but AI ticks must use {@link ZTSnapshot} instead. This
     * value is intentionally not snapshotted: urgency ordering keeps it here.
     */
    public static int alarmTicks() {
        return Math.max(20, (int) Math.round(ALARM_SECONDS.get() * 20.0D));
    }

    public static double maxZombieDamage() {
        return Z_MAX_DAMAGE_HEARTS.get() * 2.0D; // hearts -> health points
    }

    // ------------------------------------------------------------------ the two dials
    /** Effective intelligence level for a wave: base + growth, clamped to the cap. */
    public static double intelligence(int wave) {
        return Math.min(I_MAX.get(), I_BASE.get() + Math.max(0, designWave(wave)) * I_PER_WAVE.get());
    }

    /** Reach scaling from brains: intelligence×1 = 1.0×, ×3 = 1.5×, ×5 = 2.0×. */
    private static double intellectReachScale(int wave) {
        return 0.75D + 0.25D * intelligence(wave);
    }

    /** Effective frenzy multiplier during waves (0 when calm via callers passing active=false). */
    public static double frenzy() {
        return F_INTENSITY.get();
    }

    /** How often (ticks) a hunt goal re-checks for prey. Smarter + frenzied = twitchier. */
    public static int retargetIntervalTicks(int wave, boolean waveActive) {
        int interval = (int) Math.round(14.0D - 2.0D * intelligence(wave) - (waveActive ? 3.0D * frenzy() : 0.0D));
        return Math.max(2, Math.min(40, interval));
    }

    /** Ticks a zombie remembers prey it can no longer see (scales with brains + rage). */
    public static int unseenMemoryTicks(int wave, boolean waveActive) {
        int base = I_UNSEEN_MEMORY.get() + (int) Math.round(Math.max(0, designWave(wave)) * I_UNSEEN_PER_WAVE.get());
        if (waveActive) base = (int) Math.round(base * (1.0D + 0.5D * frenzy()));
        return Math.max(10, Math.min(600, base));
    }

    /** Reaction cooldown for the sense engine: smarter reacts faster, frenzy faster still. */
    public static int noiseCooldownTicks(int wave, boolean waveActive) {
        double brainFactor = Math.max(0.5D, 1.0D - 0.08D * intelligence(wave));
        double frenzyFactor = waveActive ? F_NOISE_FACTOR.get() : 1.0D;
        return Math.max(5, (int) Math.round(Z_NOISE_COOLDOWN_TICKS.get() * brainFactor * frenzyFactor));
    }

    public static double zombieSpeed(int wave, boolean waveActive) {
        double speed = Z_BASE_SPEED.get() + Math.max(0, designWave(wave) - 1) * Z_SPEED_PER_WAVE.get();
        if (waveActive) speed += F_SPEED_BOOST.get() * frenzy();
        return Math.min(Z_MAX_SPEED.get(), speed); // the 1.2x law is unbreakable
    }

    public static double followRange(int wave, boolean waveActive) {
        double v = Z_FOLLOW_RANGE.get() + Math.max(0, designWave(wave)) * Z_FOLLOW_PER_WAVE.get()
                + (waveActive ? Z_FOLLOW_WAVE_BONUS.get() * frenzy() : 0.0D);
        v *= intellectReachScale(wave);
        return Math.min(Z_FOLLOW_CAP.get(), v);
    }

    public static double hearingRadius(int wave, boolean waveActive) {
        double v = Z_HEARING_RADIUS.get() + Math.max(0, designWave(wave)) * Z_HEARING_PER_WAVE.get()
                + (waveActive ? F_HEARING_BONUS.get() * frenzy() : 0.0D);
        v *= intellectReachScale(wave);
        return Math.min(Z_HEARING_CAP.get(), v);
    }

    public static int zombieCap(int wave) {
        int v = S_CAP_PER_PLAYER.get() + (int) Math.floor(Math.max(0, wave) * S_CAP_PER_WAVE.get());
        return Math.min(S_CAP_MAX.get(), v);
    }

    public static boolean blockBreakingAllowedNow(int wave, boolean waveActive) {
        if (designWave(wave) < Z_BLOCK_BREAK_FROM_WAVE.get()) return false;
        return !Z_BLOCK_BREAK_ONLY_WAVES.get() || waveActive;
    }

    public static ResourceLocation alarmSoundId() {
        ResourceLocation rl = ResourceLocation.tryParse(ALARM_SOUND.get());
        return rl != null ? rl : ResourceLocation.fromNamespaceAndPath("zombietide", "wave_alarm");
    }

    // ------------------------------------------------------------------ parsed caches
    private static volatile List<Item> heldBlocksCache = null;
    private static volatile Set<Block> breakBlacklistCache = null;
    private static volatile Set<ResourceKey<Level>> dimensionCache = null;
    private static volatile List<EffectRoll> effectRollCache = null;
    private static volatile Map<Integer, Integer> intervalOverrideCache = null;
    private static volatile Map<Integer, Integer> durationOverrideCache = null;

    public static void onConfigLoaded(ModConfigEvent.Loading e) {
        // NeoForge fires this once per OWNED file — our CLIENT toml arrives first and
        // must NOT rebuild the COMMON-feeding snapshot (its values are not loaded yet).
        if (e.getConfig().getSpec() != SPEC) return;
        invalidateCaches();
    }

    public static void onConfigReloaded(ModConfigEvent.Reloading e) {
        if (e.getConfig().getSpec() != SPEC) return;
        invalidateCaches();
    }

    public static void invalidateCaches() {
        heldBlocksCache = null;
        breakBlacklistCache = null;
        dimensionCache = null;
        effectRollCache = null;
        intervalOverrideCache = null;
        durationOverrideCache = null;
        ZTSnapshot.refresh(); // rebake every per-wave table & hot scalar in one pass
        dev.file233.zombietide.wave.WaveManager.onConfigEdited(); // drop alarm caches too
    }

    /** Per-wave calm-gap overrides (wave → seconds). */
    public static Map<Integer, Integer> intervalOverrides() {
        Map<Integer, Integer> c = intervalOverrideCache;
        if (c == null) intervalOverrideCache = c = parseOverrides(INTERVAL_OVERRIDES.get(), "intervalOverrides");
        return c;
    }

    /** Per-wave duration overrides (wave → seconds). */
    public static Map<Integer, Integer> durationOverrides() {
        Map<Integer, Integer> c = durationOverrideCache;
        if (c == null) durationOverrideCache = c = parseOverrides(DURATION_OVERRIDES.get(), "durationOverrides");
        return c;
    }

    private static Map<Integer, Integer> parseOverrides(List<? extends String> raw, String keyName) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (String entry : raw) {
            String[] parts = entry.trim().split("\\s*[=:]\\s*");
            if (parts.length != 2) { LOGGER.warn("Bad {} entry '{}', expected wave=seconds", keyName, entry); continue; }
            try {
                int wave = Integer.parseInt(parts[0].trim());
                int seconds = Integer.parseInt(parts[1].trim());
                if (wave < 1 || seconds < 5) { LOGGER.warn("Bad {} entry '{}': wave>=1 and seconds>=5 required", keyName, entry); continue; }
                map.put(wave, seconds);
            } catch (NumberFormatException ex) {
                LOGGER.warn("Bad {} entry '{}': not a number", keyName, entry);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /** Sets/clears a per-wave override and persists it. Applies immediately to the live cycle. */
    public static synchronized void putIntervalOverride(int wave, @Nullable Integer seconds) {
        putOverride(INTERVAL_OVERRIDES, wave, seconds);
    }

    /** Sets/clears a per-wave duration override and persists it. Applies immediately. */
    public static synchronized void putDurationOverride(int wave, @Nullable Integer seconds) {
        putOverride(DURATION_OVERRIDES, wave, seconds);
    }

    private static void putOverride(ModConfigSpec.ConfigValue<List<? extends String>> entry, int wave, @Nullable Integer seconds) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (String rawEntry : entry.get()) {
            String[] parts = rawEntry.trim().split("\\s*[=:]\\s*");
            if (parts.length == 2) {
                try {
                    map.put(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (seconds == null) map.remove(wave);
        else map.put(wave, Math.max(5, seconds));
        List<String> out = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : map.entrySet()) out.add(e.getKey() + "=" + e.getValue());
        entry.set(out);
        invalidateCaches();
        saveThrottled();
    }

    // ------------------------------------------------------------------ saving
    /** Upper bound on config-file write frequency — disk IO never becomes a tick cost. */
    private static final long SAVE_DEBOUNCE_NANOS = 300_000_000L; // 300 ms
    private static volatile long lastSaveNanos = 0L;

    /** Persists the spec, coalescing bursts of rapid edits (command macros, datapacks). */
    public static void saveThrottled() {
        long now = System.nanoTime();
        if (now - lastSaveNanos >= SAVE_DEBOUNCE_NANOS) {
            lastSaveNanos = now;
            SPEC.save();
        }
    }

    public static List<Item> heldBlocks() {
        List<Item> c = heldBlocksCache;
        if (c == null) {
            List<Item> tmp = new ArrayList<>();
            for (String s : Z_HELD_BLOCKS.get()) {
                ResourceLocation id = ResourceLocation.tryParse(s);
                if (id == null) continue;
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item instanceof BlockItem && item != null && item != net.minecraft.world.item.Items.AIR) tmp.add(item);
            }
            heldBlocksCache = c = tmp.isEmpty() ? List.of(((BlockItem) net.minecraft.world.level.block.Blocks.DIRT.asItem())) : List.copyOf(tmp);
        }
        return c;
    }

    public static Set<Block> breakBlacklist() {
        Set<Block> c = breakBlacklistCache;
        if (c == null) {
            Set<Block> tmp = new LinkedHashSet<>();
            for (String s : Z_BLOCK_BREAK_BLACKLIST.get()) {
                ResourceLocation id = ResourceLocation.tryParse(s);
                if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) tmp.add(BuiltInRegistries.BLOCK.get(id));
            }
            breakBlacklistCache = c = Collections.unmodifiableSet(tmp);
        }
        return c;
    }

    public static Set<ResourceKey<Level>> dimensions() {
        Set<ResourceKey<Level>> c = dimensionCache;
        if (c == null) {
            Set<ResourceKey<Level>> tmp = new LinkedHashSet<>();
            for (String s : S_DIMENSIONS.get()) {
                ResourceLocation id = ResourceLocation.tryParse(s);
                if (id != null) tmp.add(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
            }
            if (tmp.isEmpty()) tmp.add(Level.OVERWORLD);
            dimensionCache = c = Collections.unmodifiableSet(tmp);
        }
        return c;
    }

    public static boolean dimensionAllowed(ResourceKey<Level> dim) {
        return dimensions().contains(dim);
    }

    /** One parsed entry of {@code effects.list}. */
    public static record EffectRoll(ResourceLocation id, int minSec, int maxSec, int minWave, int weight) {}

    public static List<EffectRoll> effectRolls() {
        List<EffectRoll> c = effectRollCache;
        if (c == null) {
            List<EffectRoll> tmp = new ArrayList<>();
            for (String raw : E_LIST.get()) {
                String[] parts = raw.split("\\|");
                if (parts.length != 5) { LOGGER.warn("Bad effect entry '{}', expected id|minS|maxS|minWave|weight", raw); continue; }
                try {
                    ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
                    if (id == null) { LOGGER.warn("Bad effect id in '{}'", raw); continue; }
                    tmp.add(new EffectRoll(id,
                            Math.max(0, Integer.parseInt(parts[1].trim())),
                            Math.max(0, Integer.parseInt(parts[2].trim())),
                            Math.max(1, Integer.parseInt(parts[3].trim())),
                            Math.max(1, Integer.parseInt(parts[4].trim()))));
                } catch (NumberFormatException ex) {
                    LOGGER.warn("Bad numeric field in effect entry '{}'", raw);
                }
            }
            effectRollCache = c = List.copyOf(tmp);
        }
        return c;
    }

    @Nullable
    public static net.minecraft.core.Holder<MobEffect> resolveEffect(ResourceLocation id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
    }

    // ------------------------------------------------------------------ command bridge
    /** Registry of every scalar option so /zombietide config can reach all of them. */
    public record BridgeEntry(ModConfigSpec.ConfigValue<?> value, String type) {}

    public static final Map<String, BridgeEntry> BRIDGE;

    static {
        Map<String, BridgeEntry> m = new LinkedHashMap<>();
        m.put("general.enabled", new BridgeEntry(ENABLED, "boolean"));
        m.put("waves.maxWaves", new BridgeEntry(MAX_WAVES, "int"));
        m.put("waves.firstWaveMinutes", new BridgeEntry(FIRST_WAVE_MINUTES, "double"));
        m.put("waves.waveIncrementMinutes", new BridgeEntry(WAVE_INCREMENT_MINUTES, "double"));
        m.put("waves.alarmSeconds", new BridgeEntry(ALARM_SECONDS, "double"));
        m.put("waves.alarmSound", new BridgeEntry(ALARM_SOUND, "string"));
        m.put("waves.alarmVolume", new BridgeEntry(ALARM_VOLUME, "double"));
        m.put("waves.alarmPitch", new BridgeEntry(ALARM_PITCH, "double"));
        m.put("waves.afterLastWave", new BridgeEntry(AFTER_LAST_WAVE, "enum:AfterLastWave"));
        m.put("waves.chatAnnounce", new BridgeEntry(CHAT_ANNOUNCE, "boolean"));
        m.put("waves.titleAnnounce", new BridgeEntry(TITLE_ANNOUNCE, "boolean"));
        m.put("zombies.baseSpeed", new BridgeEntry(Z_BASE_SPEED, "double"));
        m.put("zombies.maxSpeed", new BridgeEntry(Z_MAX_SPEED, "double"));
        m.put("zombies.speedPerWave", new BridgeEntry(Z_SPEED_PER_WAVE, "double"));
        m.put("zombies.followRange", new BridgeEntry(Z_FOLLOW_RANGE, "double"));
        m.put("zombies.followRangePerWave", new BridgeEntry(Z_FOLLOW_PER_WAVE, "double"));
        m.put("zombies.followRangeCap", new BridgeEntry(Z_FOLLOW_CAP, "double"));
        m.put("zombies.waveFollowBonus", new BridgeEntry(Z_FOLLOW_WAVE_BONUS, "double"));
        m.put("zombies.huntWithoutSight", new BridgeEntry(Z_HUNT_WITHOUT_SIGHT, "boolean"));
        m.put("zombies.maxDamageHearts", new BridgeEntry(Z_MAX_DAMAGE_HEARTS, "double"));
        m.put("zombies.damagePerWaveHearts", new BridgeEntry(Z_DAMAGE_PER_WAVE_HEARTS, "double"));
        m.put("zombies.healthBaseHearts", new BridgeEntry(Z_HEALTH_BASE_HEARTS, "double"));
        m.put("zombies.healthPerWaveHearts", new BridgeEntry(Z_HEALTH_PER_WAVE_HEARTS, "double"));
        m.put("zombies.healthMaxHeartsAbovePlayer", new BridgeEntry(Z_HEALTH_MAX_ABOVE_PLAYER, "double"));
        m.put("zombies.knockbackResistancePerWave", new BridgeEntry(Z_KBR_PER_WAVE, "double"));
        m.put("zombies.reinforcementFromWave", new BridgeEntry(Z_REINFORCEMENT_FROM_WAVE, "int"));
        m.put("zombies.reinforcementPerWave", new BridgeEntry(Z_REINFORCEMENT_PER_WAVE, "double"));
        m.put("zombies.reinforcementCap", new BridgeEntry(Z_REINFORCEMENT_CAP, "double"));
        m.put("zombies.hearingRadius", new BridgeEntry(Z_HEARING_RADIUS, "double"));
        m.put("zombies.hearingPerWave", new BridgeEntry(Z_HEARING_PER_WAVE, "double"));
        m.put("zombies.hearingCap", new BridgeEntry(Z_HEARING_CAP, "double"));
        m.put("zombies.noiseCooldownTicks", new BridgeEntry(Z_NOISE_COOLDOWN_TICKS, "int"));
        m.put("zombies.stripArmor", new BridgeEntry(Z_STRIP_ARMOR, "boolean"));
        m.put("zombies.allowOnlyBlockItems", new BridgeEntry(Z_BLOCK_ITEMS_ONLY, "boolean"));
        m.put("zombies.heldBlockChance", new BridgeEntry(Z_HELD_BLOCK_CHANCE, "double"));
        m.put("zombies.blockBreakFromWave", new BridgeEntry(Z_BLOCK_BREAK_FROM_WAVE, "int"));
        m.put("zombies.blockBreakChance", new BridgeEntry(Z_BLOCK_BREAK_CHANCE, "double"));
        m.put("zombies.blockBreakMaxHardness", new BridgeEntry(Z_BLOCK_BREAK_MAX_HARDNESS, "double"));
        m.put("zombies.blockBreakCooldownTicks", new BridgeEntry(Z_BLOCK_BREAK_COOLDOWN, "int"));
        m.put("zombies.blockBreakOnlyDuringWaves", new BridgeEntry(Z_BLOCK_BREAK_ONLY_WAVES, "boolean"));
        m.put("zombies.noBurningDuringWaves", new BridgeEntry(Z_NO_BURN_IN_WAVES, "boolean"));
        m.put("zombies.babyKeepChance", new BridgeEntry(Z_BABY_KEEP_CHANCE, "double"));
        m.put("zombies.variantKeepChance", new BridgeEntry(Z_VARIANT_KEEP_CHANCE, "double"));
        m.put("zombies.nonZombieKeepChance", new BridgeEntry(Z_NON_ZOMBIE_KEEP_CHANCE, "double"));
        m.put("zombies.convertVariants", new BridgeEntry(Z_CONVERT_VARIANTS, "boolean"));
        m.put("targeting.targetCreativePlayers", new BridgeEntry(T_CREATIVE, "boolean"));
        m.put("targeting.targetSpectators", new BridgeEntry(T_SPECTATORS, "boolean"));
        m.put("intelligence.baseLevel", new BridgeEntry(I_BASE, "double"));
        m.put("intelligence.perWaveBonus", new BridgeEntry(I_PER_WAVE, "double"));
        m.put("intelligence.maxLevel", new BridgeEntry(I_MAX, "double"));
        m.put("intelligence.unseenMemoryTicks", new BridgeEntry(I_UNSEEN_MEMORY, "int"));
        m.put("intelligence.unseenMemoryPerWave", new BridgeEntry(I_UNSEEN_PER_WAVE, "int"));
        m.put("frenzy.intensity", new BridgeEntry(F_INTENSITY, "double"));
        m.put("frenzy.speedBoost", new BridgeEntry(F_SPEED_BOOST, "double"));
        m.put("frenzy.hearingBonus", new BridgeEntry(F_HEARING_BONUS, "double"));
        m.put("frenzy.noiseReactionFactor", new BridgeEntry(F_NOISE_FACTOR, "double"));
        m.put("spawning.enabled", new BridgeEntry(S_ENABLED, "boolean"));
        m.put("spawning.intervalTicks", new BridgeEntry(S_INTERVAL_TICKS, "int"));
        m.put("spawning.attemptsPerCycle", new BridgeEntry(S_ATTEMPTS_PER_CYCLE, "int"));
        m.put("spawning.ringMinDistance", new BridgeEntry(S_RING_MIN, "int"));
        m.put("spawning.ringMaxDistance", new BridgeEntry(S_RING_MAX, "int"));
        m.put("spawning.capPerPlayer", new BridgeEntry(S_CAP_PER_PLAYER, "int"));
        m.put("spawning.capPerPlayerPerWave", new BridgeEntry(S_CAP_PER_WAVE, "double"));
        m.put("spawning.capMax", new BridgeEntry(S_CAP_MAX, "int"));
        m.put("spawning.daylightSpawn", new BridgeEntry(S_DAYLIGHT_SPAWN, "boolean"));
        m.put("spawning.boostNaturalPlacement", new BridgeEntry(S_BOOST_NATURAL_PLACEMENT, "boolean"));
        m.put("spawning.surfaceChance", new BridgeEntry(S_SURFACE_CHANCE, "double"));
        m.put("spawning.ignoreDoMobSpawningRule", new BridgeEntry(S_IGNORE_GAMERULE, "boolean"));
        m.put("spawning.pressureCreativePlayers", new BridgeEntry(S_PRESSURE_CREATIVE, "boolean"));
        m.put("spawning.daySpawnFactor", new BridgeEntry(S_DAY_SPAWN_FACTOR, "double"));
        m.put("effects.procChance", new BridgeEntry(E_PROC_CHANCE, "double"));
        m.put("effects.onlyDuringWaves", new BridgeEntry(E_ONLY_DURING_WAVES, "boolean"));
        m.put("effects.amplifier", new BridgeEntry(E_AMPLIFIER, "int"));
        BRIDGE = Collections.unmodifiableMap(m);
    }

    /** Human-readable current value for a bridge key. */
    public static String bridgeGet(String key) {
        BridgeEntry e = BRIDGE.get(key);
        return e == null ? null : String.valueOf(e.value().get());
    }

    /** Sets a bridge key from raw text. @return null on success, otherwise an error message. */
    @Nullable
    public static String bridgeSet(String key, String raw) {
        BridgeEntry e = BRIDGE.get(key);
        if (e == null) return "unknown_key";
        try {
            switch (e.type()) {
                case "boolean" -> {
                    if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) return "needs true/false";
                    ((ModConfigSpec.BooleanValue) e.value()).set(Boolean.parseBoolean(raw));
                }
                case "int" -> ((ModConfigSpec.ConfigValue<Integer>) cast(e.value())).set(Integer.parseInt(raw));
                case "double" -> ((ModConfigSpec.ConfigValue<Double>) cast(e.value())).set(Double.parseDouble(raw));
                case "string" -> ((ModConfigSpec.ConfigValue<String>) cast(e.value())).set(raw);
                case "enum:AfterLastWave" -> ((ModConfigSpec.ConfigValue<AfterLastWave>) cast(e.value()))
                        .set(AfterLastWave.valueOf(raw.toUpperCase(java.util.Locale.ROOT)));
                default -> { return "readonly"; }
            }
        } catch (IllegalArgumentException ex) {
            return "bad_value: " + ex.getMessage();
        }
        invalidateCaches();
        saveThrottled();
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) { return (T) o; }
}
