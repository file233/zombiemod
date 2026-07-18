package dev.file233.zombietide.client;

import dev.file233.zombietide.util.ZTTime;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * The tiny tactical HUD pinned to the top-center of the screen.
 *
 * <p>Calm: fills its little progress bar as the next wave approaches, counting down with
 * days/hours/minutes/seconds. Alarm: flashes amber. Wave: turns red and drains the bar as
 * the siege burns out. After the final wave: a small golden memorial. Always tiny.
 */
public final class ZTHudLayer implements LayeredDraw.Layer {
    public static final ZTHudLayer INSTANCE = new ZTHudLayer();

    private static final int COLOR_BG = 0x8C000000;
    private static final int COLOR_BORDER = 0x33FFFFFF;
    private static final int COLOR_BAR_BG = 0x55000000;
    private static final int COLOR_CALM = 0xFF66BB6A;
    private static final int COLOR_ALARM = 0xFFFFB300;
    private static final int COLOR_WAR = 0xFFEF5350;
    private static final int COLOR_GOLD = 0xFFFFD54F;
    private static final int COLOR_TEXT = 0xFFF3F3F3;
    private static final int COLOR_TEXT_DIM = 0xFFBDBDBD;

    @Override
    public void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;
        if (!ZTClientConfig.HUD_ENABLED.get() || !ZTClientState.hasData) return;

        float scale = (float) ZTClientConfig.HUD_SCALE.get().doubleValue();
        int offsetY = ZTClientConfig.HUD_OFFSET_Y.get();

        String title = titleText();
        String time = timeText();
        float progress = progress();

        int barColor = barColor();
        int textColor = ZTClientState.alarmSounding && blink() ? COLOR_ALARM : COLOR_TEXT;

        int screenW = mc.getWindow().getGuiScaledWidth();
        float cx = screenW / 2.0F;

        var pose = gui.pose();
        pose.pushPose();
        pose.translate(cx, offsetY, 0.0F);
        pose.scale(scale, scale, 1.0F);

        int half = 78;
        int height = 27;
        // card
        gui.fill(-half, 0, half, height, COLOR_BG);
        gui.fill(-half, 0, half, 1, COLOR_BORDER);
        gui.fill(-half, height - 1, half, height, COLOR_BORDER);

        // line 1: wave label
        gui.drawCenteredString(mc.font, title, 0, 3, textColor);
        // line 2: countdown
        gui.drawCenteredString(mc.font, time, 0, 13, ZTClientState.finished ? COLOR_GOLD : COLOR_TEXT_DIM);

        // line 3: progress bar
        int barHalf = 72;
        gui.fill(-barHalf, 22, barHalf, 25, COLOR_BAR_BG);
        int fillTo = -barHalf + Math.round(progress * barHalf * 2.0F);
        if (fillTo > -barHalf) gui.fill(-barHalf, 22, fillTo, 25, barColor);

        pose.popPose();
    }

    private static boolean blink() {
        return Util.getMillis() % 1000L < 500L;
    }

    private static String titleText() {
        if (ZTClientState.finished) {
            return Component.translatable("hud.zombietide.finished", ZTClientState.maxWaves, ZTClientState.maxWaves).getString();
        }
        int shown = Math.min(ZTClientState.waveActive() ? ZTClientState.wave : ZTClientState.wave + 1, ZTClientState.maxWaves);
        String suffix = ZTClientConfig.HUD_SHOW_ZOMBIE_COUNT.get() && ZTClientState.waveActive()
                ? " | " + Component.translatable("hud.zombietide.zombies", ZTClientState.zombiesAlive).getString()
                : "";
        Component key = ZTClientState.waveActive()
                ? Component.translatable("hud.zombietide.wave", shown, ZTClientState.maxWaves)
                : Component.translatable("hud.zombietide.next", shown, ZTClientState.maxWaves);
        String paused = ZTClientState.paused ? " | " + Component.translatable("hud.zombietide.paused").getString() : "";
        return key.getString() + suffix + paused;
    }

    private static String timeText() {
        long ticks = ZTClientState.ticksRemaining;
        if (ZTClientState.finished) return Component.translatable("hud.zombietide.done").getString();
        if (ZTClientState.alarmSounding) return "!! " + (ticks / 20L + 1) + " !!";
        return ZTClientConfig.HUD_TIME_FORMAT.get() == ZTClientConfig.HudTimeFormat.INGAME
                ? ZTTime.formatIngame(ticks)
                : ZTTime.formatRealtime(ticks);
    }

    private static float progress() {
        if (ZTClientState.finished) return 1.0F;
        if (ZTClientState.paused) return 0.5F;
        float frac = (float) ZTClientState.ticksRemaining / (float) ZTClientState.phaseTotal;
        frac = Math.min(1.0F, Math.max(0.0F, frac));
        // calm fills up toward the wave; war drains down to the relief
        return ZTClientState.waveActive() ? frac : 1.0F - frac;
    }

    private static int barColor() {
        if (ZTClientState.finished) return COLOR_GOLD;
        if (ZTClientState.paused) return COLOR_TEXT_DIM;
        if (ZTClientState.waveActive()) return COLOR_WAR;
        if (ZTClientState.alarmSounding) return blink() ? COLOR_ALARM : COLOR_WAR;
        return COLOR_CALM;
    }
}
