package dev.file233.zombietide.client;

import dev.file233.zombietide.network.WaveSyncPayload;

/**
 * Entry points invoked from networking lambda glue. Kept tiny and signature-safe so the
 * class can be *referenced* (never executed) on dedicated servers without harm.
 */
public final class ZTClientHooks {
    private ZTClientHooks() {}

    public static void handleWaveSync(WaveSyncPayload payload) {
        ZTClientState.apply(payload);
    }
}
