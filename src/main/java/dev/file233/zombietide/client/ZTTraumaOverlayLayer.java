package dev.file233.zombietide.client;

import java.util.Random;

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
 *
 * <p><b>Performance:</b> the whole droplet constellation — positions, sizes, shades,
 * per-droplet translucency — is rolled <i>once</i> per hit into flat int arrays and then
 * reused every frame until the next hit (seed change), a resize, or a config tweak.
 * Rendering a frame costs exactly two GPU quads… times a few dozen integer fills with
 * zero RNG, zero branching math and zero string/composite work.
 */
public final class ZTTraumaOverlayLayer implements LayeredDraw.Layer {
    public static final ZTTraumaOverlayLayer INSTANCE = new ZTTraumaOverlayLayer();

    // ---- droplet cache, keyed by (seed, screen size, droplet dials) ----
    private long cachedSeed = Long.MIN_VALUE;
    private int cachedW = -1;
    private int cachedH = -1;
    private int cachedCfgCount = -1;
    private int cachedCfgSize = -1;
    private int cachedCfgSpread = -1;
    private int[] xs = new int[0];
    private int[] ys = new int[0];
    private int[] sizes = new int[0];
    private int[] rgbs = new int[0];      // opaque 0xRRGGBB shades
    private int[] alphaF = new int[0];    // per-droplet translucency 140..254
    private int capacity = 0;

    @Override
    public void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        if (!ZTClientConfig.OVERLAY_ENABLED.get()) return;

        float trauma = ZTClientState.trauma;
        if (trauma <= 0.02F) return;

        ensureCache(mc);
        if (capacity <= 0) return;

        float alphaScale = Math.min((float) ZTClientConfig.OVERLAY_MAX_ALPHA.get().doubleValue(), trauma);
        int droplets = Math.round(cachedCfgCount * trauma);
        droplets = Math.min(droplets, capacity);
        if (droplets <= 0) return;

        // per-frame draw: tiny squares only; droplets drain from the end of the (random-order) array
        for (int i = 0; i < droplets; i++) {
            int alpha = (int) (alphaScale * alphaF[i]);
            gui.fill(xs[i], ys[i], xs[i] + sizes[i], ys[i] + sizes[i], (alpha << 24) | rgbs[i]);
        }
    }

    /**
     * Rebuilds the splatter when the hit seed, window size or any droplet dial changes.
     * Everything random lives here — frames between hits are pure cache draws.
     */
    private void rebuild(int w, int h, int count, int baseSize, int spread) {
        Random rand = new Random(ZTClientState.splatterSeed);

        // 2–4 impact points on the glass
        int clusters = 2 + rand.nextInt(3);
        int[] cx = new int[clusters];
        int[] cy = new int[clusters];
        for (int i = 0; i < clusters; i++) {
            cx[i] = rand.nextInt(Math.max(1, w));
            cy[i] = rand.nextInt(Math.max(1, h));
        }

        xs = new int[count];
        ys = new int[count];
        sizes = new int[count];
        rgbs = new int[count];
        alphaF = new int[count];
        for (int i = 0; i < count; i++) {
            int c = rand.nextInt(clusters);
            xs[i] = cx[c] + (int) Math.round(rand.nextGaussian() * spread);
            ys[i] = cy[c] + (int) Math.round(rand.nextGaussian() * spread);
            sizes[i] = Math.max(1, baseSize + rand.nextInt(3) - 1);
            int red = 120 + rand.nextInt(95);              // dark-to-fresh blood shades
            int green = 4 + rand.nextInt(16);
            int blue = 8 + rand.nextInt(14);
            rgbs[i] = (red << 16) | (green << 8) | blue;
            alphaF[i] = 140 + rand.nextInt(115);           // varied translucency
        }
        capacity = count;
        cachedSeed = ZTClientState.splatterSeed;
        cachedW = w;
        cachedH = h;
        cachedCfgCount = count;
        cachedCfgSize = baseSize;
        cachedCfgSpread = spread;
    }

    /** Cache gate: rebuild exactly on a new hit seed, a resize, or a droplet-dial edit. */
    public void ensureCache(Minecraft mc) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int count = Math.max(1, ZTClientConfig.OVERLAY_DROPLET_COUNT.get().intValue());
        int size = Math.max(1, ZTClientConfig.OVERLAY_DROPLET_SIZE.get().intValue());
        int spread = Math.max(4, ZTClientConfig.OVERLAY_DROPLET_SPREAD.get().intValue());
        if (ZTClientState.splatterSeed != cachedSeed || w != cachedW || h != cachedH
                || count != cachedCfgCount || size != cachedCfgSize || spread != cachedCfgSpread) {
            rebuild(w, h, count, size, spread);
        }
    }
}
