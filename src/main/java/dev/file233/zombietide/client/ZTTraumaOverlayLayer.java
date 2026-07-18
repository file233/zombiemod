package dev.file233.zombietide.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * Blood on the lens — as discrete pixel droplets. Each damaging hit throws a small,
 * deliberately SPARSE constellation of square blood pixels onto the screen from one
 * fixed seed (so a hit paints a stable splatter, not shimmering noise). Droplets cling
 * to a few impact points, vary in shade and size, and drain away one by one as the
 * trauma fades. Few pixels, sharp pain — no heavy blur, no full-screen tint.
 * Every dial (count, size, spread, opacity, fade) lives in the client config.
 */
public final class ZTTraumaOverlayLayer implements LayeredDraw.Layer {
    public static final ZTTraumaOverlayLayer INSTANCE = new ZTTraumaOverlayLayer();

    @Override
    public void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        if (!ZTClientConfig.OVERLAY_ENABLED.get()) return;

        float trauma = ZTClientState.trauma;
        if (trauma <= 0.02F) return;

        float alphaScale = Math.min((float) ZTClientConfig.OVERLAY_MAX_ALPHA.get().doubleValue(), trauma);
        int droplets = Math.round((float) ZTClientConfig.OVERLAY_DROPLET_COUNT.get().intValue() * trauma);
        if (droplets <= 0) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int baseSize = Math.max(1, ZTClientConfig.OVERLAY_DROPLET_SIZE.get());
        int spread = Math.max(4, ZTClientConfig.OVERLAY_DROPLET_SPREAD.get());

        // deterministic per hit: same seed -> same constellation every frame (no flicker)
        java.util.Random rand = new java.util.Random(ZTClientState.splatterSeed);

        // 2–4 impact points on the glass
        int clusters = 2 + rand.nextInt(3);
        int[] cx = new int[clusters];
        int[] cy = new int[clusters];
        for (int i = 0; i < clusters; i++) {
            cx[i] = rand.nextInt(Math.max(1, w));
            cy[i] = rand.nextInt(Math.max(1, h));
        }

        for (int i = 0; i < droplets; i++) {
            int c = rand.nextInt(clusters);
            int x = cx[c] + (int) Math.round(rand.nextGaussian() * spread);
            int y = cy[c] + (int) Math.round(rand.nextGaussian() * spread);
            int size = Math.max(1, baseSize + rand.nextInt(3) - 1);
            int red = 120 + rand.nextInt(95);              // dark-to-fresh blood shades
            int green = 4 + rand.nextInt(16);
            int blue = 8 + rand.nextInt(14);
            int alpha = (int) (alphaScale * (140 + rand.nextInt(115))); // varied translucency
            gui.fill(x, y, x + size, y + size, (alpha << 24) | (red << 16) | (green << 8) | blue);
        }
    }
}
