package dev.file233.zombietide.client;

import dev.file233.zombietide.ZombieTide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client bootstrap: in-game config screen (Mod List &rarr; ZombieTide &rarr; Config),
 * the two GUI layers (wave HUD + red fog), and the trauma/damage watcher.
 */
@Mod(value = ZombieTide.MOD_ID, dist = Dist.CLIENT)
public final class ZombieTideClient {
    public ZombieTideClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ZTClientConfig.SPEC);

        // the professional in-game config UI (covers COMMON + CLIENT of this mod)
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(ZombieTideClient::onRegisterGuiLayers);
        modEventBus.addListener(ZombieTideClient::onClientConfigReloaded);
        NeoForge.EVENT_BUS.addListener(ZombieTideClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ZombieTideClient::onLoggingOut);
    }

    /** Config-screen saves / file edits: the HUD rebuilds its cached strings next frame. */
    private static void onClientConfigReloaded(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ZTClientConfig.SPEC) {
            ZTHudLayer.INSTANCE.invalidate();
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // order matters: the fog sits above the world, the HUD rides on top of everything
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(ZombieTide.MOD_ID, "trauma_overlay"),
                ZTTraumaOverlayLayer.INSTANCE);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(ZombieTide.MOD_ID, "wave_hud"),
                ZTHudLayer.INSTANCE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            ZTClientState.tickDamageWatch(player);
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ZTClientState.reset();
    }
}
