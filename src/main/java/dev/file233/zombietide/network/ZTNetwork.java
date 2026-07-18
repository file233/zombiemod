package dev.file233.zombietide.network;

import dev.file233.zombietide.client.ZTClientHooks;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Network registration. One channel, one direction — the HUD stream. */
public final class ZTNetwork {
    public static final String PROTOCOL = "1";

    private ZTNetwork() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(WaveSyncPayload.TYPE, WaveSyncPayload.STREAM_CODEC,
                // Handler class is client-only code but never *executes* on a dedicated server:
                // playToClient channels are only invoked on the receiving (client) side.
                (payload, context) -> context.enqueueWork(() -> ZTClientHooks.handleWaveSync(payload)));
    }
}
