package dev.file233.zombietide.registry;

import dev.file233.zombietide.ZombieTide;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Transient per-entity runtime state (NeoForge attachment system).
 * Neither attachment is persisted: they are tactical bookkeeping only.
 */
public final class ZTAttachments {
    private ZTAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ZombieTide.MOD_ID);

    /** Last game-time this zombie reacted to a noise (sound-alert cooldown). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> NOISE_COOLDOWN =
            ATTACHMENTS.register("noise_cooldown", () -> AttachmentType.builder(() -> 0L).build());

    /** True when a zombie has already had the wave-AI goals injected (avoids double-adding). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> WAVE_TAGGED =
            ATTACHMENTS.register("wave_tagged", () -> AttachmentType.builder(() -> false).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
