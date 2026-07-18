package dev.file233.zombietide.command;

import java.util.Locale;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.config.ZTSnapshot;
import dev.file233.zombietide.util.ZTTime;
import dev.file233.zombietide.wave.WaveData;
import dev.file233.zombietide.wave.WaveManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /zombietide} — full remote control of the apocalypse.
 *
 * <pre>
 *   status                  → where in the cycle we are
 *   start|summon [instant]  → ring the sirene (or skip it with "instant")
 *   end                     → finish the running wave now
 *   wave &lt;n&gt; [instant]     → jump to wave n
 *   interval [n] [sec|clear]→ calm gap per wave: view effective, set or clear override
 *   duration [n] [sec|clear]→ wave length per wave: view effective, set or clear override
 *   reset                   → back to day one (wave 1 countdown)
 *   pause / resume          → freeze / unfreeze the whole cycle
 *   config list [filter]    → every tunable key
 *   config get &lt;key&gt;        → current value
 *   config set &lt;key&gt; &lt;v&gt;   → change + persist to the config file, live
 * </pre>
 */
public final class ZTCommands {
    private ZTCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("zombietide");

        root.then(Commands.literal("status").executes(ZTCommands::status));

        root.then(Commands.literal("start").requires(src -> src.hasPermission(2))
                .executes(ctx -> trigger(ctx, false))
                .then(Commands.literal("instant").executes(ctx -> trigger(ctx, true))));
        root.then(Commands.literal("summon").requires(src -> src.hasPermission(2))
                .executes(ctx -> trigger(ctx, false))
                .then(Commands.literal("instant").executes(ctx -> trigger(ctx, true))));

        root.then(Commands.literal("end").requires(src -> src.hasPermission(2)).executes(ZTCommands::end));

        root.then(Commands.literal("wave").requires(src -> src.hasPermission(2))
                .then(Commands.argument("number", IntegerArgumentType.integer(1, 100000))
                        .executes(ctx -> wave(ctx, false))
                        .then(Commands.literal("instant").executes(ctx -> wave(ctx, true)))));

        // per-wave calm gap: list / show / set / clear (persisted into waves.intervalOverrides)
        var interval = Commands.literal("interval").executes(ctx -> overview(ctx, true));
        interval.then(Commands.argument("wave", IntegerArgumentType.integer(1, 100000))
                .executes(ctx -> showPerWave(ctx, true))
                .then(Commands.literal("clear").requires(src -> src.hasPermission(2))
                        .executes(ctx -> setPerWave(ctx, true, null)))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 120960000))
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> setPerWave(ctx, true, IntegerArgumentType.getInteger(ctx, "seconds")))));
        root.then(interval);

        // per-wave duration: list / show / set / clear (persisted into waves.durationOverrides)
        var duration = Commands.literal("duration").executes(ctx -> overview(ctx, false));
        duration.then(Commands.argument("wave", IntegerArgumentType.integer(1, 100000))
                .executes(ctx -> showPerWave(ctx, false))
                .then(Commands.literal("clear").requires(src -> src.hasPermission(2))
                        .executes(ctx -> setPerWave(ctx, false, null)))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 120960000))
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> setPerWave(ctx, false, IntegerArgumentType.getInteger(ctx, "seconds")))));
        root.then(duration);

        root.then(Commands.literal("reset").requires(src -> src.hasPermission(2))
                .executes(ZTCommands::reset));

        root.then(Commands.literal("pause").requires(src -> src.hasPermission(2))
                .executes(ctx -> pause(ctx, true)));
        root.then(Commands.literal("resume").requires(src -> src.hasPermission(2))
                .executes(ctx -> pause(ctx, false)));

        var config = Commands.literal("config");
        config.then(Commands.literal("list")
                .executes(ctx -> list(ctx, ""))
                .then(Commands.argument("filter", StringArgumentType.word())
                        .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "filter")))));
        config.then(Commands.literal("get")
                .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ZTConfig.BRIDGE.keySet(), b))
                        .executes(ZTCommands::get)));
        config.then(Commands.literal("set").requires(src -> src.hasPermission(2))
                .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ZTConfig.BRIDGE.keySet(), b))
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .suggests((c, b) -> {
                                    var entry = ZTConfig.BRIDGE.get(StringArgumentType.getString(c, "key"));
                                    if (entry != null && entry.type().equals("boolean")) {
                                        return SharedSuggestionProvider.suggest(new String[]{"true", "false"}, b);
                                    }
                                    if (entry != null && entry.type().startsWith("enum:")) {
                                        return SharedSuggestionProvider.suggest(new String[]{"CONTINUE", "LOOP", "STOP"}, b);
                                    }
                                    return b.buildFuture();
                                })
                                .executes(ZTCommands::set))));
        root.then(config);

        var rootNode = event.getDispatcher().register(root);
        // convenience alias
        event.getDispatcher().register(Commands.literal("zt").redirect(rootNode));
    }

    // ------------------------------------------------------------------ actions
    private static int status(CommandContext<CommandSourceStack> ctx) {
        WaveManager manager = WaveManager.get();
        if (manager == null) {
            ctx.getSource().sendFailure(Component.translatable("command.zombietide.no_manager"));
            return 0;
        }
        WaveData d = manager.data();
        int max = ZTSnapshot.get().maxWaves;
        if (d.finished) {
            send(ctx, ChatFormatting.GOLD, Component.translatable("command.zombietide.status.finished", d.completed, max));
            return 1;
        }
        if (d.phase == WaveData.PHASE_ACTIVE) {
            send(ctx, ChatFormatting.RED, Component.translatable(
                    "command.zombietide.status.active", d.wave, max, ZTTime.formatRealtime(d.ticksRemaining)));
        } else if (d.paused) {
            send(ctx, ChatFormatting.YELLOW, Component.translatable(
                    "command.zombietide.status.paused", d.wave + 1, max, ZTTime.formatRealtime(d.ticksRemaining)));
        } else {
            send(ctx, ChatFormatting.GREEN, Component.translatable(
                    "command.zombietide.status.idle", d.wave + 1, max, ZTTime.formatRealtime(d.ticksRemaining)));
        }
        send(ctx, ChatFormatting.GRAY, Component.translatable(
                "command.zombietide.status.detail", manager.zombiesAlive(), d.completed));
        boolean rageOn = d.phase == WaveData.PHASE_ACTIVE && !d.paused;
        ZTSnapshot snap = ZTSnapshot.get();
        send(ctx, ChatFormatting.LIGHT_PURPLE, Component.translatable(
                rageOn ? "command.zombietide.status.mind_active" : "command.zombietide.status.mind_calm",
                formatMinutes(snap.intelligenceFor(d.wave)), formatMinutes(snap.frenzyIntensity)));
        return 1;
    }

    private static int trigger(CommandContext<CommandSourceStack> ctx, boolean instant) {
        WaveManager manager = WaveManager.get();
        if (manager == null) { ctx.getSource().sendFailure(Component.translatable("command.zombietide.no_manager")); return 0; }
        String error = manager.triggerWave(instant);
        if (error != null) {
            ctx.getSource().sendFailure(Component.translatable("command.zombietide.fail." + error));
            return 0;
        }
        send(ctx, ChatFormatting.YELLOW, Component.translatable(
                instant ? "command.zombietide.start_instant" : "command.zombietide.start", Math.min(manager.data().wave + 1, ZTConfig.MAX_WAVES.get())));
        return 1;
    }

    private static int end(CommandContext<CommandSourceStack> ctx) {
        WaveManager manager = WaveManager.get();
        if (manager == null) { ctx.getSource().sendFailure(Component.translatable("command.zombietide.no_manager")); return 0; }
        String error = manager.endWaveNow();
        if (error != null) {
            ctx.getSource().sendFailure(Component.translatable("command.zombietide.fail." + error));
            return 0;
        }
        send(ctx, ChatFormatting.GREEN, Component.translatable("command.zombietide.end", manager.data().wave));
        return 1;
    }

    private static int wave(CommandContext<CommandSourceStack> ctx, boolean instant) {
        WaveManager manager = WaveManager.get();
        if (manager == null) { ctx.getSource().sendFailure(Component.translatable("command.zombietide.no_manager")); return 0; }
        int number = IntegerArgumentType.getInteger(ctx, "number");
        String error = manager.jumpToWave(number, instant);
        if (error != null) {
            ctx.getSource().sendFailure(Component.translatable("command.zombietide.fail." + error));
            return 0;
        }
        send(ctx, ChatFormatting.YELLOW, Component.translatable("command.zombietide.wave", number));
        return 1;
    }

    // ------------------------------------------------------------------ per-wave interval / duration
    private static int overview(CommandContext<CommandSourceStack> ctx, boolean calmGap) {
        String key = calmGap ? "interval" : "duration";
        var overrides = calmGap ? ZTConfig.intervalOverrides() : ZTConfig.durationOverrides();
        if (calmGap) {
            send(ctx, ChatFormatting.AQUA, Component.translatable("command.zombietide.interval.header",
                    ZTConfig.defaultCalmSeconds()));
        } else {
            send(ctx, ChatFormatting.AQUA, Component.translatable("command.zombietide.duration.header",
                    formatMinutes(ZTConfig.FIRST_WAVE_MINUTES.get()), formatMinutes(ZTConfig.WAVE_INCREMENT_MINUTES.get())));
        }
        if (overrides.isEmpty()) {
            send(ctx, ChatFormatting.GRAY, Component.translatable("command.zombietide." + key + ".none"));
        } else {
            for (var e : overrides.entrySet()) {
                int wave = e.getKey();
                long ticks = e.getValue() * 20L;
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "command.zombietide." + key + ".override_line", wave, ZTTime.formatRealtime(ticks))
                        .withStyle(ChatFormatting.GOLD), false);
            }
        }
        send(ctx, ChatFormatting.DARK_GRAY, Component.translatable("command.zombietide." + key + ".hint"));
        return overrides.size() + 1;
    }

    private static int showPerWave(CommandContext<CommandSourceStack> ctx, boolean calmGap) {
        int wave = IntegerArgumentType.getInteger(ctx, "wave");
        long ticks = calmGap ? ZTConfig.calmTicks(wave) : ZTConfig.waveDurationTicks(wave);
        String source = calmGap ? ZTConfig.calmSource(wave) : ZTConfig.durationSource(wave);
        send(ctx, ChatFormatting.GREEN, Component.translatable(
                "command.zombietide." + (calmGap ? "interval" : "duration") + ".show",
                wave, ZTTime.formatRealtime(ticks), source));
        return 1;
    }

    private static int setPerWave(CommandContext<CommandSourceStack> ctx, boolean calmGap, Integer seconds) {
        int wave = IntegerArgumentType.getInteger(ctx, "wave");
        String key = calmGap ? "interval" : "duration";
        if (seconds == null) {
            if (calmGap) ZTConfig.putIntervalOverride(wave, null);
            else ZTConfig.putDurationOverride(wave, null);
            send(ctx, ChatFormatting.YELLOW, Component.translatable("command.zombietide." + key + ".cleared", wave));
        } else {
            if (calmGap) ZTConfig.putIntervalOverride(wave, seconds);
            else ZTConfig.putDurationOverride(wave, seconds);
            send(ctx, ChatFormatting.GREEN, Component.translatable(
                    "command.zombietide." + key + ".set_ok", wave, ZTTime.formatRealtime(seconds * 20L)));
        }
        // feel the surgery immediately: live-retune the countdown it concerns
        WaveManager manager = WaveManager.get();
        if (manager != null) {
            if (calmGap) manager.retuneCalm();
            else manager.retuneActive(wave);
        }
        return 1;
    }

    private static String formatMinutes(double minutes) {
        String s = String.format(Locale.ROOT, "%.1f", minutes);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        WaveManager manager = WaveManager.get();
        if (manager == null) { ctx.getSource().sendFailure(Component.translatable("command.zombietide.no_manager")); return 0; }
        manager.resetProgress();
        send(ctx, ChatFormatting.GOLD, Component.translatable("command.zombietide.reset"));
        return 1;
    }

    private static int pause(CommandContext<CommandSourceStack> ctx, boolean pause) {
        WaveManager manager = WaveManager.get();
        if (manager == null) { ctx.getSource().sendFailure(Component.translatable("command.zombietide.no_manager")); return 0; }
        manager.setPaused(pause);
        send(ctx, ChatFormatting.YELLOW, Component.translatable(pause ? "command.zombietide.pause" : "command.zombietide.resume"));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx, String filter) {
        String f = filter.toLowerCase(Locale.ROOT);
        send(ctx, ChatFormatting.AQUA, Component.translatable("command.zombietide.config.header"));
        int shown = 0;
        for (Map.Entry<String, ZTConfig.BridgeEntry> e : ZTConfig.BRIDGE.entrySet()) {
            if (!f.isEmpty() && !e.getKey().toLowerCase(Locale.ROOT).contains(f)) continue;
            shown++;
            String finalKey = e.getKey();
            String finalVal = ZTConfig.bridgeGet(finalKey);
            ctx.getSource().sendSuccess(() -> Component.literal(" §7- §f" + finalKey + " §8= §b" + finalVal), false);
        }
        if (shown == 0) ctx.getSource().sendFailure(Component.translatable("command.zombietide.config.none"));
        return shown;
    }

    private static int get(CommandContext<CommandSourceStack> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        String value = ZTConfig.bridgeGet(key);
        if (value == null) {
            ctx.getSource().sendFailure(Component.translatable("command.zombietide.config.unknown", key));
            return 0;
        }
        send(ctx, ChatFormatting.AQUA, Component.literal(key + " = " + value));
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        String value = StringArgumentType.getString(ctx, "value").trim();
        String error = ZTConfig.bridgeSet(key, value);
        if (error != null) {
            ctx.getSource().sendFailure(Component.translatable("command.zombietide.config.set_fail", key, error));
            return 0;
        }
        send(ctx, ChatFormatting.GREEN, Component.translatable("command.zombietide.config.set_ok", key, ZTConfig.bridgeGet(key)));
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, ChatFormatting color, Component component) {
        Component colored = component.copy().withStyle(color);
        ctx.getSource().sendSuccess(() -> colored, false);
    }
}
