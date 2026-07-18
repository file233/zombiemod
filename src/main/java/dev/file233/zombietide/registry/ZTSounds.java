package dev.file233.zombietide.registry;

import dev.file233.zombietide.ZombieTide;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom sounds shipped by ZombieTide. */
public final class ZTSounds {
    private ZTSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, ZombieTide.MOD_ID);

    /** 5-second air-raid siren played {@link #ALARM_SECONDS} before each wave. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WAVE_ALARM = SOUNDS.register("wave_alarm",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieTide.MOD_ID, "wave_alarm")));

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
