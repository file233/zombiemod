package dev.file233.zombietide;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import dev.file233.zombietide.command.ZTCommands;
import dev.file233.zombietide.config.ZTConfig;
import dev.file233.zombietide.network.ZTNetwork;
import dev.file233.zombietide.registry.ZTAttachments;
import dev.file233.zombietide.registry.ZTSounds;
import dev.file233.zombietide.wave.WaveManager;
import dev.file233.zombietide.zombie.CombatRules;
import dev.file233.zombietide.zombie.SenseEngine;
import dev.file233.zombietide.zombie.SpawnFilter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * ZombieTide — a relentless, escalating zombie apocalypse for Minecraft 1.21.1 (NeoForge).
 *
 * <p>Feature map (mirrors the design brief):
 * <ul>
 *   <li>Rebuilt zombie senses: long-range no-line-of-sight hunting, sound awareness, frenzy states.</li>
 *   <li>Timed wave system: wave 1 lasts 8 minutes, every next wave +2 minutes, 50 waves by default.</li>
 *   <li>Daylight-capable natural spawning during waves, zombies never burn in sunlight mid-wave.</li>
 *   <li>Strict spawn hygiene mid-wave: only plain zombies; variants & other hostiles almost never.</li>
 *   <li>Hard caps: max 2 hearts of zombie damage, max 1.2x player speed, no armor (blocks only).</li>
 *   <li>From wave 20 some zombies break blocks; hits can inflict random harmful effects.</li>
 *   <li>Tiny top-center HUD with day/hour/minute/second countdown + progress bar.</li>
 *   <li>Everything configurable in-game (Mod List &rarr; Config) and via /zombietide commands.</li>
 * </ul>
 */
@Mod(ZombieTide.MOD_ID)
public final class ZombieTide {
    public static final String MOD_ID = "zombietide";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ZombieTide(IEventBus modEventBus, ModContainer modContainer) {
        // ---- configuration (everything is editable in-game via NeoForge's config screen) ----
        modContainer.registerConfig(ModConfig.Type.COMMON, ZTConfig.SPEC);

        // ---- registries ----
        ZTSounds.register(modEventBus);
        ZTAttachments.register(modEventBus);

        // ---- networking ----
        modEventBus.addListener(ZTNetwork::onRegisterPayloads);

        // ---- config file load/reload (these fire on the MOD bus) ----
        modEventBus.addListener(ZTConfig::onConfigLoaded);
        modEventBus.addListener(ZTConfig::onConfigReloaded);

        // ---- game bus wiring ----
        IEventBus game = NeoForge.EVENT_BUS;
        // wave core
        game.addListener(WaveManager::onServerStarted);
        game.addListener(WaveManager::onServerStopping);
        game.addListener(WaveManager::onServerTick);
        game.addListener(WaveManager::onPlayerLoggedIn);
        // zombie spawning rules
        game.addListener(SpawnFilter::onFinalizeSpawn);
        game.addListener(SpawnFilter::onEntityJoinLevel);
        game.addListener(SpawnFilter::onSpawnPlacementCheck);
        game.addListener(SpawnFilter::onPositionCheck);
        // combat: damage cap + harmful effects + sun-fire purge
        game.addListener(CombatRules::onIncomingDamage);
        game.addListener(CombatRules::onEntityTick);
        // senses: the things zombies can "hear"
        game.addListener(SenseEngine::onPlayerTick);
        game.addListener(SenseEngine::onLivingJump);
        game.addListener(SenseEngine::onBlockBreak);
        game.addListener(SenseEngine::onBlockPlace);
        game.addListener(SenseEngine::onAttackEntity);
        game.addListener(SenseEngine::onProjectileImpact);
        game.addListener(SenseEngine::onUseItemFinish);
        game.addListener(SenseEngine::onRightClickBlock);
        game.addListener(SenseEngine::onExplosionDetonate);
        // commands
        game.addListener(ZTCommands::onRegisterCommands);

        LOGGER.info("ZombieTide {} loaded — the tide is coming.", System.getProperty("zombietide.version", "dev"));
    }
}
