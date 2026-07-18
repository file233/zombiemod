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

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.translation("zombietide.configuration.client.hud").push("hud");
        HUD_ENABLED = b.comment("Show the tiny wave HUD (top-center of the screen).",
                "Hiện HUD đợt thu nhỏ ở giữa cạnh trên màn hình.").translation("zombietide.configuration.client.enabled").define("enabled", true);
        HUD_SCALE = b.comment("HUD scale. It is meant to be TINY; crank it down, not up.",
                "Tỉ lệ HUD — vốn dĩ là cực nhỏ.").translation("zombietide.configuration.client.scale").defineInRange("scale", 0.7D, 0.3D, 2.0D);
        HUD_OFFSET_Y = b.comment("Vertical offset in pixels from the top edge.",
                "Độ lệch dọc so với cạnh trên (px).").translation("zombietide.configuration.client.offsetY").defineInRange("offsetY", 3, 0, 200);
        HUD_SHOW_ZOMBIE_COUNT = b.comment("Show live zombie headcount.",
                "Hiện số zombie đang sống.").translation("zombietide.configuration.client.showZombieCount").define("showZombieCount", true);
        HUD_TIME_FORMAT = b.comment("Countdown format: REALTIME (20 ticks = 1s) or INGAME (24000 ticks = 1 MC day).",
                "Định dạng đếm ngược: thởi gian thật hoặc ngày trong game.").translation("zombietide.configuration.client.timeFormat").defineEnum("timeFormat", HudTimeFormat.REALTIME);
        b.pop();

        b.translation("zombietide.configuration.client.damageOverlay").push("damageOverlay");
        OVERLAY_ENABLED = b.comment("Red fog/blur on your screen when you take heavy damage.",
                "Màn hình mờ đỏ khi bị sát thương mạnh.").translation("zombietide.configuration.client.enabled").define("enabled", true);
        OVERLAY_INTENSITY = b.comment("How strongly damage feeds the red fog.",
                "Độ mạnh hiệu ứng.").translation("zombietide.configuration.client.intensity").defineInRange("intensity", 1.0D, 0.1D, 4.0D);
        OVERLAY_MAX_ALPHA = b.comment("Strongest opacity the red fog can reach (1 = fully blinding).",
                "Độ đục tối đa.").translation("zombietide.configuration.client.maxAlpha").defineInRange("maxAlpha", 0.65D, 0.1D, 1.0D);
        b.pop();

        SPEC = b.build();
    }
}
