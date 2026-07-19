package dev.file233.zombietide.client;

import dev.file233.zombietide.ZombieTide;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/**
 * Blood on the lens: every serious hit throws a fresh blood-splatter pattern at the
 * screen, washed in a layered red blur (several slightly-overscaled, low-alpha passes
 * stack into a soft, foggy bleed — the screen literally looks smeared in gore). The gore
 * drains away slowly while you hold together. Three texture variants cycle at random so
 * no two scrapes look alike. Driven by the local trauma accumulator (charged on every
 * health drop, decaying over time) — every dial lives in the client config.
 */
public final class ZTTraumaOverlayLayer implements LayeredDraw.Layer {
    public static final ZTTraumaOverlayLayer INSTANCE = new ZTTraumaOverlayLayer();

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[ZTClientState.SPLATTER_VARIANTS];

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(
                    ZombieTide.MOD_ID, "textures/gui/trauma_overlay_" + i + ".png");
        }
    }

    @Override
    public void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        if (!ZTClientConfig.OVERLAY_ENABLED.get()) return;

        float trauma = ZTClientState.trauma;
        if (trauma <= 0.01F) return;

        // soft pulse so the blood feels freshly pumping
        float pulse = 0.90F + 0.10F * (float) Math.sin(net.minecraft.Util.getMillis() / 150.0D);
        float alpha = Math.min((float) ZTClientConfig.OVERLAY_MAX_ALPHA.get().doubleValue(), trauma * pulse);
        if (alpha <= 0.01F) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int splatter = Math.min(ZTClientState.splatterIndex, TEXTURES.length - 1);
        int passes = Math.max(1, ZTClientConfig.OVERLAY_BLUR_PASSES.get());

        // blur stack: pass 1 is the crisp splatter; the following passes bleed outward
        // wider and fainter, which reads as a genuine smeared-out-of-focus red haze.
        for (int i = passes - 1; i >= 0; i--) {
            float growth = 9.0F * i;                       // outward bleed per pass (px)
            float layerAlpha = i == 0 ? alpha : alpha * (0.42F / i);
            gui.setColor(1.0F, 1.0F, 1.0F, layerAlpha);
            gui.blit(TEXTURES[splatter],
                    (int) -growth, (int) -growth,
                    (int) (w + 2.0F * growth), (int) (h + 2.0F * growth),
                    0.0F, 0.0F, 512, 512, 512, 512);
        }
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
