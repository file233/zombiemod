package dev.file233.zombietide.wave;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.network.WaveSyncPayload;
import dev.file233.zombietide.zombie.ZombieMutation;
import dev.file233.zombietide.registry.ZTSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The apocalypse conductor: owns the wave cycle (calm countdown &rarr; alarm &rarr; siege &rarr;
 * brief respite), escalates zombie intelligence after every survived wave, and broadcasts
 * HUD snapshots to every client once per second (and on every transition).
 *
 * <p>Timeline per cycle (all values configurable):
 * <pre>
 *  calmMinutes of peace  ──[alarmSeconds of sirene]──▶  wave #N (8 + 2·(N-1) minutes)  ──▶  calmMinutes …
 * </pre>
 * Default: 50 waves; behaviour after wave 50 is governed by {@code waves.afterLastWave}.
 */
public final class WaveManager {
    private static final int SYNC_PERIOD_TICKS = 20;

    @Nullable
    private static WaveManager instance;

    public static void onServerStarted(ServerStartedEvent event) {
        instance = new WaveManager(event.getServer());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        instance = null;
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (instance != null && event.getEntity() instanceof ServerPlayer sp) {
            instance.syncTo(sp);
        }
    }

    @Nullable
    public static WaveManager get() {
        return instance;
    }

    /** Fast static query used by hot paths (spawn filter, combat rules, senses). */
    public static boolean isWaveActive() {
        WaveManager m = instance;
        return m != null && ZTConfig.enabled() && m.data.phase == WaveData.PHASE_ACTIVE && !m.data.paused;
    }

    /** Current wave number (0 before the first wave). */
    public static int currentWave() {
        WaveManager m = instance;
        return m == null ? 0 : m.data.wave;
    }

    // ------------------------------------------------------------------ instance state
    private final MinecraftServer server;
    private final WaveData data;
    private boolean alarmConsumed = false; // transient: the sirene fires once per calm window
    private int syncTimer = 0;
    private int zombiesAlive = 0; // refreshed every SYNC_PERIOD_TICKS

    private WaveManager(MinecraftServer server) {
        this.server = server;
        this.data = server.overworld().getDataStorage().computeIfAbsent(WaveData.factory(), WaveData.DATA_NAME);
        if (data.phaseTotal <= 0) {
            data.phaseTotal = ZTConfig.calmTicks();
            data.ticksRemaining = data.phaseTotal;
        }
    }

    public WaveData data() { return data; }

    public boolean isPaused() { return data.paused; }
    public boolean isFinished() { return data.finished; }
    public int zombiesAlive() { return zombiesAlive; }

    // ------------------------------------------------------------------ ticking
    public static void onServerTick(ServerTickEvent.Post event) {
        WaveManager m = instance;
        if (m == null || !ZTConfig.enabled()) return;
        if (m.server != event.getServer()) return; // defensive
        m.tick();
    }

    private void tick() {
        if (data.paused || data.finished) {
            throttledSync();
            return;
        }

        if (data.phase == WaveData.PHASE_IDLE) {
            // sirene exactly alarmSeconds before the wave
            if (!alarmConsumed && data.ticksRemaining <= ZTConfig.alarmTicks() && data.ticksRemaining > 0) {
                fireAlarm();
            }
            if (--data.ticksRemaining <= 0) {
                startWave();
            }
        } else {
            WaveSpawner.tick(server, data.wave);
            if (--data.ticksRemaining <= 0) {
                endWave();
            }
        }
        throttledSync();
    }

    private void throttledSync() {
        if (syncTimer-- <= 0) {
            syncTimer = SYNC_PERIOD_TICKS;
            refreshZombieCount();
            syncAll();
        }
    }

    // ------------------------------------------------------------------ transitions
    private void startWave() {
        int max = ZTConfig.MAX_WAVES.get();
        int next = data.wave + 1;
        if (next > max) {
            switch (ZTConfig.AFTER_LAST_WAVE.get()) {
                case STOP -> {
                    data.finished = true;
                    data.phase = WaveData.PHASE_IDLE;
                    data.phaseTotal = 1;
                    data.ticksRemaining = 1;
                    data.setDirty();
                    announce(ChatFormatting.GOLD, "message.zombietide.apocalypse_survived",
                            "message.zombietide.apocalypse_survived.sub", max);
                    syncAll();
                    return;
                }
                case LOOP -> next = 1;
                case CONTINUE -> next = max;
                default -> next = max;
            }
        }
        data.wave = next;
        data.phase = WaveData.PHASE_ACTIVE;
        data.phaseTotal = ZTConfig.waveDurationTicks(next);
        data.ticksRemaining = data.phaseTotal;
        alarmConsumed = false;
        data.setDirty();

        if (ZTConfig.CHAT_ANNOUNCE.get()) {
            broadcast(Component.translatable("message.zombietide.wave_started", next, max)
                    .withStyle(ChatFormatting.DARK_RED));
        }
        if (ZTConfig.TITLE_ANNOUNCE.get()) {
            announce(ChatFormatting.RED, "message.zombietide.wave_started.title",
                    "message.zombietide.wave_started.sub", next, max);
        }
        // Frenzy! Re-mutate every loaded zombie for the new intelligence level.
        ZombieMutation.sweep(server);
        syncAll();
    }

    private void endWave() {
        data.completed = Math.max(data.completed, data.wave);
        data.phase = WaveData.PHASE_IDLE;
        data.phaseTotal = ZTConfig.calmTicks();
        data.ticksRemaining = data.phaseTotal;
        data.setDirty();

        if (ZTConfig.CHAT_ANNOUNCE.get()) {
            broadcast(Component.translatable("message.zombietide.wave_survived", data.wave, ZTConfig.MAX_WAVES.get())
                    .withStyle(ChatFormatting.GREEN));
        }
        if (ZTConfig.TITLE_ANNOUNCE.get()) {
            announce(ChatFormatting.GREEN, "message.zombietide.wave_survived.title",
                    "message.zombietide.wave_survived.sub", data.wave, ZTConfig.MAX_WAVES.get());
        }
        // The horde calms down (but stays smarter than ever).
        ZombieMutation.sweep(server);
        syncAll();
    }

    private void fireAlarm() {
        alarmConsumed = true;
        Holder<SoundEvent> sound = resolveAlarmSound();
        float volume = (float) ZTConfig.ALARM_VOLUME.get().doubleValue();
        int next = Math.min(data.wave + 1, ZTConfig.MAX_WAVES.get());
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (!ZTConfig.dimensionAllowed(sp.level().dimension())) continue;
            // played at each player's own position so everyone hears the full sirene
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), sound, SoundSource.HOSTILE, volume, 1.0F);
        }
        if (ZTConfig.CHAT_ANNOUNCE.get()) {
            broadcast(Component.translatable("message.zombietide.incoming", next, ZTConfig.MAX_WAVES.get())
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (ZTConfig.TITLE_ANNOUNCE.get()) {
            announce(ChatFormatting.YELLOW, "message.zombietide.incoming.title",
                    "message.zombietide.incoming.sub", next, ZTConfig.MAX_WAVES.get());
        }
        syncAll();
    }

    private Holder<SoundEvent> resolveAlarmSound() {
        ResourceLocation id = ZTConfig.alarmSoundId();
        return BuiltInRegistries.SOUND_EVENT.getHolder(id)
                .map(ref -> (Holder<SoundEvent>) ref)
                .orElse(ZTSounds.WAVE_ALARM);
    }

    // ------------------------------------------------------------------ command surface
    /** Triggers the next wave: full alarm first, or instantly when {@code instant}. */
    public String triggerWave(boolean instant) {
        if (data.finished) return "finished";
        if (data.phase == WaveData.PHASE_ACTIVE) return "already_active";
        if (instant) {
            startWave();
        } else {
            data.ticksRemaining = Math.min(data.ticksRemaining, ZTConfig.alarmTicks());
            alarmConsumed = false;
            data.setDirty();
        }
        syncAll();
        return null;
    }

    /** Ends the running wave immediately. */
    public String endWaveNow() {
        if (data.phase != WaveData.PHASE_ACTIVE) return "not_active";
        data.ticksRemaining = 0;
        endWave();
        return null;
    }

    /** Skips forward to a specific wave and starts it. */
    public String jumpToWave(int wave, boolean instant) {
        int max = ZTConfig.MAX_WAVES.get();
        data.wave = Math.max(0, Math.min(wave - 1, max - 1));
        data.finished = false;
        if (data.phase != WaveData.PHASE_ACTIVE) {
            data.phase = WaveData.PHASE_IDLE;
            data.phaseTotal = ZTConfig.calmTicks();
            data.ticksRemaining = data.phaseTotal;
        }
        return triggerWave(instant);
    }

    /** Resets the whole apocalypse back to day one. */
    public void resetProgress() {
        data.wave = 0;
        data.completed = 0;
        data.phase = WaveData.PHASE_IDLE;
        data.phaseTotal = ZTConfig.calmTicks();
        data.ticksRemaining = data.phaseTotal;
        data.paused = false;
        data.finished = false;
        alarmConsumed = false;
        data.setDirty();
        ZombieMutation.sweep(server);
        syncAll();
    }

    public void setPaused(boolean paused) {
        data.paused = paused;
        data.setDirty();
        syncAll();
    }

    // ------------------------------------------------------------------ announce & sync
    private void broadcast(Component component) {
        server.getPlayerList().broadcastSystemMessage(component, false);
    }

    private void announce(ChatFormatting color, String titleKey, String subtitleKey, Object... args) {
        Component title = Component.translatable(titleKey, args).withStyle(color);
        Component subtitle = Component.translatable(subtitleKey, args).withStyle(ChatFormatting.GRAY);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (!ZTConfig.dimensionAllowed(sp.level().dimension())) continue;
            sp.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            sp.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            sp.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    private void refreshZombieCount() {
        Set<UUID> seen = new HashSet<>();
        List<ServerLevel> dims = new ArrayList<>();
        for (var key : ZTConfig.dimensions()) {
            ServerLevel level = server.getLevel(key);
            if (level != null) dims.add(level);
        }
        for (ServerLevel level : dims) {
            for (ServerPlayer sp : level.players()) {
                for (Zombie z : level.getEntitiesOfClass(Zombie.class, sp.getBoundingBox().inflate(128.0D))) {
                    seen.add(z.getUUID());
                }
            }
        }
        zombiesAlive = seen.size();
    }

    private void syncAll() {
        WaveSyncPayload payload = snapshot();
        PacketDistributor.sendToAllPlayers(payload);
    }

    private void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot());
    }

    private WaveSyncPayload snapshot() {
        byte flags = (byte) ((data.paused ? 1 : 0) | (data.finished ? 2 : 0) | (alarmConsumed && data.phase == WaveData.PHASE_IDLE ? 4 : 0));
        return new WaveSyncPayload(data.wave, ZTConfig.MAX_WAVES.get(), (byte) data.phase,
                Math.max(0L, data.ticksRemaining), Math.max(1L, data.phaseTotal), zombiesAlive, flags);
    }
}
