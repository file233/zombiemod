package dev.file233.zombietide.client;

import dev.file233.zombietide.ZombieTide;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/**
 * The red fog of war: when the horde bites deep, your vision drowns in a pulsing
 * red-out vignette that slowly recedes as you hold together. Driven by the local
 * trauma accumulator (charged on every health drop, decaying over time).
 */
public final class ZTTraumaOverlayLayer implements LayeredDraw.Layer {
    public static final ZTTraumaOverlayLayer INSTANCE = new ZTTraumaOverlayLayer();

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ZombieTide.MOD_ID, "textures/gui/trauma_overlay.png");

    @Override
    public void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        if (!ZTClientConfig.OVERLAY_ENABLED.get()) return;

        float trauma = ZTClientState.trauma;
        if (trauma <= 0.01F) return;

        // soft pulse so the fog feels alive
        float pulse = 0.92F + 0.08F * (float) Math.sin((Util_millis()) / 180.0D);
        float alpha = Math.min((float) ZTClientConfig.OVERLAY_MAX_ALPHA.get().doubleValue(), trauma * pulse);
        if (alpha <= 0.01F) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        gui.setColor(1.0F, 1.0F, 1.0F, alpha);
        gui.blit(TEXTURE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static long Util_millis() {
        return net.minecraft.Util.getMillis();
    }
}
