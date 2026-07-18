package dev.file233.zombietide.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * CLIENT-side cosmetic options (HUD, red trauma overlay). CLIENT type so the in-game
 * config screen works the same in singleplayer and on servers — these never need to
 * punch through to the server, they only describe how YOUR screen draws the apocalypse.
 */
public final class ZTClientConfig {
    private ZTClientConfig() {}

    public enum HudTimeFormat { REALTIME, INGAME }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ModConfigSpec.BooleanValue HUD_SHOW_ZOMBIE_COUNT;
    public static final ModConfigSpec.ConfigValue<HudTimeFormat> HUD_TIME_FORMAT;
    public static final ModConfigSpec.BooleanValue OVERLAY_ENABLED;
    public static final ModConfigSpec.DoubleValue OVERLAY_INTENSITY;
    public static final ModConfigSpec.DoubleValue OVERLAY_MAX_ALPHA;
    public static final ModConfigSpec.DoubleValue OVERLAY_FADE_PER_TICK;
    public static final ModConfigSpec.IntValue OVERLAY_DROPLET_COUNT;
    public static final ModConfigSpec.IntValue OVERLAY_DROPLET_SIZE;
    public static final ModConfigSpec.IntValue OVERLAY_DROPLET_SPREAD;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.translation("zombietide.configuration.client.hud").push("hud");
        HUD_ENABLED = b.comment("Show the tiny wave HUD (top-center of the screen).",
                "Hiện HUD đợt thu nhỏ ở giữa cạnh trên màn hình.").translation("zombietide.configuration.client.enabled").define("enabled", true);
        HUD_SCALE = b.comment("HUD scale. It is meant to be TINY; crank it down, not up.",
                "Tỉ lệ HUD — vốn dĩ là cực nhỏ.").translation("zombietide.configuration.client.scale").defineInRange("scale", 0.7D, 0.003D, 200.0D);
        HUD_OFFSET_Y = b.comment("Vertical offset in pixels from the top edge.",
                "Độ lệch dọc so với cạnh trên (px).").translation("zombietide.configuration.client.offsetY").defineInRange("offsetY", 3, 0, 20000);
        HUD_SHOW_ZOMBIE_COUNT = b.comment("Show live zombie headcount.",
                "Hiện số zombie đang sống.").translation("zombietide.configuration.client.showZombieCount").define("showZombieCount", true);
        HUD_TIME_FORMAT = b.comment("Countdown format: REALTIME (20 ticks = 1s) or INGAME (24000 ticks = 1 MC day).",
                "Định dạng đếm ngược: thởi gian thật hoặc ngày trong game.").translation("zombietide.configuration.client.timeFormat").defineEnum("timeFormat", HudTimeFormat.REALTIME);
        b.pop();

        b.translation("zombietide.configuration.client.damageOverlay").push("damageOverlay");
        OVERLAY_ENABLED = b.comment("Sparse blood pixel-droplets on your screen when you take damage.",
                "Hạt máu pixel thưa bắn lên màn hình khi bị đánh.").translation("zombietide.configuration.client.enabled").define("enabled", true);
        OVERLAY_INTENSITY = b.comment("How strongly damage feeds the droplets.",
                "Độ mạnh hiệu ứng.").translation("zombietide.configuration.client.intensity").defineInRange("intensity", 1.0D, 0.001D, 400.0D);
        OVERLAY_MAX_ALPHA = b.comment("Strongest opacity the droplets can reach (1 = fully opaque blood).",
                "Độ đục tối đa của hạt máu.").translation("zombietide.configuration.client.maxAlpha").defineInRange("maxAlpha", 0.85D, 0.001D, 100.0D);
        OVERLAY_FADE_PER_TICK = b.comment("How fast the droplets drain off your vision (per tick).",
                "Lower = the bloodied pixels linger longer.",
                "Tốc độ hạt máu nhạt dần mỗi tick (nhỏ = bám lâu hơn).")
                .translation("zombietide.configuration.client.fadePerTick").defineInRange("fadePerTick", 0.004D, 0.000005D, 5.0D);
        OVERLAY_DROPLET_COUNT = b.comment("Droplets thrown at the screen by a full-strength hit (kept SPARSE on purpose).",
                "Số hạt máu của cú đánh mạnh nhất (cố ý thưa — ít nhưng đau).")
                .translation("zombietide.configuration.client.dropletCount").defineInRange("dropletCount", 22, 1, 2200);
        OVERLAY_DROPLET_SIZE = b.comment("Base droplet size in GUI pixels (each droplet varies ±2).",
                "Kích thước cơ bản của hạt máu (px GUI, mỗi hạt dao động ±2).")
                .translation("zombietide.configuration.client.dropletSize").defineInRange("dropletSize", 3, 1, 300);
        OVERLAY_DROPLET_SPREAD = b.comment("How far droplets may scatter from their cluster centers (px).",
                "Độ tản của các cụm hạt (px).")
                .translation("zombietide.configuration.client.dropletSpread").defineInRange("dropletSpread", 70, 1, 7000);
        b.pop();

        SPEC = b.build();
    }
}
