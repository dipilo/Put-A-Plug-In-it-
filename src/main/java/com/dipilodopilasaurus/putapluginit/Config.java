/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import com.dipilodopilasaurus.putapluginit.config.ConfigSpecSink;
import com.dipilodopilasaurus.putapluginit.config.ConfigView;

public final class Config {

    private static final String P_SC_ENABLED = "leakFixes.sophisticatedCore.itemStackKeyCache.enabled";
    private static final String P_SC_CLEAR_EVERY_TICKS = "leakFixes.sophisticatedCore.itemStackKeyCache.client.clearEveryTicks";
    private static final String P_SC_MIN_SIZE = "leakFixes.sophisticatedCore.itemStackKeyCache.client.minSizeBeforeClear";
    private static final String P_SC_CLEAR_ON_SERVER = "leakFixes.sophisticatedCore.itemStackKeyCache.server.clearOnTickEnd";

    private static final boolean D_SC_ENABLED = true;
    private static final int D_SC_CLEAR_EVERY_TICKS = 20;
    private static final int D_SC_MIN_SIZE = 10_000;
    private static final boolean D_SC_CLEAR_ON_SERVER = false;

    private static final String P_LEAK_COMMANDS = "leakTools.commands.enabled";
    private static final String P_HEAP_DUMP_COMMAND = "leakTools.commands.heapDump.enabled";

    private static final boolean D_LEAK_COMMANDS = true;
    private static final boolean D_HEAP_DUMP_COMMAND = false;

    private static final String P_MM_BLOCKSTATE_ON_LEAVE = "leakFixes.minecraft.modelManager.blockStateToIdMap.clearOnClientWorldLeave";
    private static final String P_MM_BAKED_ON_LEAVE = "leakFixes.minecraft.modelManager.bakedModelMaps.clearOnClientWorldLeave";
    private static final String P_MM_BAKED_MIN_SIZE = "leakFixes.minecraft.modelManager.bakedModelMaps.client.minSizeBeforeClear";

    private static final boolean D_MM_BLOCKSTATE_ON_LEAVE = false;
    private static final boolean D_MM_BAKED_ON_LEAVE = true;
    private static final int D_MM_BAKED_MIN_SIZE = 10_000;

    private static final String P_COMPAT_LAYER = "compatibility.layer.enabled";
    private static final String P_COMPAT_LOG_MODS = "compatibility.layer.logDetectedMods";
    private static final String P_IF_AUTO_PATCH = "compatibility.immediatelyFast.autoPatchConfig";
    private static final String P_IF_DISABLE_SCREEN_BATCHING = "compatibility.immediatelyFast.disableScreenBatching";
    private static final String P_IF_DISABLE_SCREEN_BATCHING_INTEL_UHD = "compatibility.immediatelyFast.disableScreenBatchingOnIntelUhd";
    private static final String P_IF_DISABLE_FBO_SWITCHING_INTEL = "compatibility.immediatelyFast.disableAvoidRedundantFramebufferSwitchingOnIntelGpu";
    private static final String P_IF_DISABLE_HUD_BATCHING = "compatibility.immediatelyFast.disableHudBatching";
    private static final String P_MLF_DISABLE_IF_SCREEN_BATCHING = "compatibility.memoryLeakFix.disableImmediatelyFastScreenBatching";

    private static final boolean D_COMPAT_LAYER = true;
    private static final boolean D_COMPAT_LOG_MODS = true;
    private static final boolean D_IF_AUTO_PATCH = true;
    private static final boolean D_IF_DISABLE_SCREEN_BATCHING = false;
    private static final boolean D_IF_DISABLE_SCREEN_BATCHING_INTEL_UHD = true;
    private static final boolean D_IF_DISABLE_FBO_SWITCHING_INTEL = true;
    private static final boolean D_IF_DISABLE_HUD_BATCHING = false;
    private static final boolean D_MLF_DISABLE_IF_SCREEN_BATCHING = true;

    // The keys below are for fixes harvested from AllTheLeaks (MIT); see NOTICE.md.
    private static final String P_EMI_CLEAR_HISTORY = "leakFixes.mods.emi.clearHistoryOnRespawn";
    private static final String P_JADE_CLEAR_CACHES = "leakFixes.mods.jade.clearCachesOnWorldChange";
    private static final String P_ICEBERG_CLEAR_CACHES = "leakFixes.mods.iceberg.clearCachesOnWorldUnload";
    private static final String P_JEI_CLEAR_CACHES = "leakFixes.mods.jei.clearCachesOnWorldChange";
    private static final String P_SUPPLEMENTARIES_CLEAR_CACHES = "leakFixes.mods.supplementaries.clearCachesOnServerStop";
    private static final String P_SOPHCORE_CLEAR_WRAPPERS = "leakFixes.mods.sophisticatedCore.clearStorageWrappersOnWorldUnload";
    private static final String P_ETF_CLEAR_TEXTURES = "leakFixes.mods.entityTextureFeatures.clearPlayerTextureMapOnRespawn";
    private static final String P_ETF_CLEAR_RENDERER = "leakFixes.mods.entityTextureFeatures.clearRendererStateOnRenderEnd";
    private static final String P_EMF_CLEAR_RENDER_CONTEXT = "leakFixes.mods.entityModelFeatures.clearRenderContextOnRenderEnd";
    private static final String P_SOPHCORE_RESET_CRAFTING_UI = "leakFixes.mods.sophisticatedCore.resetCraftingUiOnWorldChange";
    private static final String P_MC_EXPIRE_DAMAGE_SOURCE = "leakFixes.minecraft.livingEntity.expireLastDamageSource";

    private static final boolean D_EMI_CLEAR_HISTORY = true;
    private static final boolean D_JADE_CLEAR_CACHES = true;
    private static final boolean D_ICEBERG_CLEAR_CACHES = true;
    private static final boolean D_JEI_CLEAR_CACHES = true;
    private static final boolean D_SUPPLEMENTARIES_CLEAR_CACHES = true;
    private static final boolean D_SOPHCORE_CLEAR_WRAPPERS = true;
    private static final boolean D_ETF_CLEAR_TEXTURES = true;
    private static final boolean D_ETF_CLEAR_RENDERER = true;
    private static final boolean D_EMF_CLEAR_RENDER_CONTEXT = true;
    private static final boolean D_SOPHCORE_RESET_CRAFTING_UI = true;
    private static final boolean D_MC_EXPIRE_DAMAGE_SOURCE = true;

    // Defaults stand until loadAll runs, so a fix behaves sanely before any backend is up.
    private static volatile boolean fixSophisticatedCoreItemStackKeyCache = D_SC_ENABLED;
    private static volatile int scItemStackKeyCacheClientClearEveryTicks = D_SC_CLEAR_EVERY_TICKS;
    private static volatile int scItemStackKeyCacheClientMinSizeBeforeClear = D_SC_MIN_SIZE;
    private static volatile boolean scItemStackKeyCacheClearOnServerTickEnd = D_SC_CLEAR_ON_SERVER;

    private static volatile boolean leakCommandsEnabled = D_LEAK_COMMANDS;
    private static volatile boolean heapDumpCommandEnabled = D_HEAP_DUMP_COMMAND;

    private static volatile boolean clearModelManagerBlockStateToIdMapOnClientWorldLeave = D_MM_BLOCKSTATE_ON_LEAVE;
    private static volatile boolean clearModelManagerBakedModelMapsOnClientWorldLeave = D_MM_BAKED_ON_LEAVE;
    private static volatile int clearModelManagerBakedModelMapsMinSize = D_MM_BAKED_MIN_SIZE;

    private static volatile boolean compatibilityLayerEnabled = D_COMPAT_LAYER;
    private static volatile boolean compatibilityLogDetectedMods = D_COMPAT_LOG_MODS;
    private static volatile boolean immediatelyFastCompatAutoPatchConfig = D_IF_AUTO_PATCH;
    private static volatile boolean immediatelyFastCompatDisableScreenBatching = D_IF_DISABLE_SCREEN_BATCHING;
    private static volatile boolean immediatelyFastCompatDisableScreenBatchingOnIntelUhd = D_IF_DISABLE_SCREEN_BATCHING_INTEL_UHD;
    private static volatile boolean immediatelyFastCompatDisableAvoidRedundantFramebufferSwitchingOnIntelGpu = D_IF_DISABLE_FBO_SWITCHING_INTEL;
    private static volatile boolean immediatelyFastCompatDisableHudBatching = D_IF_DISABLE_HUD_BATCHING;
    private static volatile boolean memoryLeakFixCompatDisableImmediatelyFastScreenBatching = D_MLF_DISABLE_IF_SCREEN_BATCHING;

    private static volatile boolean emiClearHistoryOnRespawn = D_EMI_CLEAR_HISTORY;
    private static volatile boolean jadeClearCachesOnWorldChange = D_JADE_CLEAR_CACHES;
    private static volatile boolean icebergClearCachesOnWorldUnload = D_ICEBERG_CLEAR_CACHES;
    private static volatile boolean jeiClearCachesOnWorldChange = D_JEI_CLEAR_CACHES;
    private static volatile boolean supplementariesClearCachesOnServerStop = D_SUPPLEMENTARIES_CLEAR_CACHES;
    private static volatile boolean sophCoreClearStorageWrappersOnWorldUnload = D_SOPHCORE_CLEAR_WRAPPERS;
    private static volatile boolean etfClearPlayerTextureMapOnRespawn = D_ETF_CLEAR_TEXTURES;
    private static volatile boolean etfClearRendererStateOnRenderEnd = D_ETF_CLEAR_RENDERER;
    private static volatile boolean emfClearRenderContextOnRenderEnd = D_EMF_CLEAR_RENDER_CONTEXT;
    private static volatile boolean sophCoreResetCraftingUiOnWorldChange = D_SOPHCORE_RESET_CRAFTING_UI;
    private static volatile boolean expireLastDamageSource = D_MC_EXPIRE_DAMAGE_SOURCE;

    private Config() {
    }

    public static void defineAll(ConfigSpecSink s) {
        s.bool(P_SC_ENABLED, D_SC_ENABLED,
                "Leak fix: clear SophisticatedCore ItemStackKey cache periodically on the client to prevent unbounded growth.",
                "SophisticatedCore already clears this cache every server tick; clearing it on the client is the primary mitigation.");
        s.integer(P_SC_CLEAR_EVERY_TICKS, D_SC_CLEAR_EVERY_TICKS, 1, 20_000,
                "How often to clear SophisticatedCore ItemStackKey cache on the CLIENT (in ticks).",
                "Set to 1 to clear every tick (most aggressive, may cause UI lag).");
        s.integer(P_SC_MIN_SIZE, D_SC_MIN_SIZE, 0, 5_000_000,
                "Only clear the SophisticatedCore ItemStackKey cache if it has at least this many entries.");
        s.bool(P_SC_CLEAR_ON_SERVER, D_SC_CLEAR_ON_SERVER,
                "Also clear SophisticatedCore ItemStackKey cache on the SERVER tick end.",
                "Not usually needed (SophisticatedCore already clears it every server tick).",
                "Enable only for debugging or if SophisticatedCore changes behavior.");

        s.bool(P_LEAK_COMMANDS, D_LEAK_COMMANDS,
                "Enable /papi leak commands for quick memory/leak diagnostics.");
        s.bool(P_HEAP_DUMP_COMMAND, D_HEAP_DUMP_COMMAND,
                "Enable /papi leak dumpHeap (writes a .hprof). Disabled by default because it can freeze the game and produce huge files.");

        s.bool(P_MM_BLOCKSTATE_ON_LEAVE, D_MM_BLOCKSTATE_ON_LEAVE,
                "Mitigation: when leaving a client world (back to menu), attempt to clear the ModelManager BlockState->int cache if detected.",
                "This is an opt-in, best-effort reflection-based cleanup. It should be safe, but may be ineffective if the MAT suspects are not caused by this cache.");
        s.bool(P_MM_BAKED_ON_LEAVE, D_MM_BAKED_ON_LEAVE,
                "MemoryLeakFix-derived mitigation: when leaving a client world, attempt to clear large ModelManager baked-model maps.",
                "This targets stale client-side model caches that may retain many entries across world transitions.");
        s.integer(P_MM_BAKED_MIN_SIZE, D_MM_BAKED_MIN_SIZE, 0, 5_000_000,
                "Only clear ModelManager baked-model maps that have at least this many entries.");

        s.bool(P_COMPAT_LAYER, D_COMPAT_LAYER,
                "Enable Put A Plug In it's compatibility layer for interoperability with other optimization mods.");
        s.bool(P_COMPAT_LOG_MODS, D_COMPAT_LOG_MODS,
                "Log detected compatibility targets (ImmediatelyFast) at startup.");
        s.bool(P_IF_AUTO_PATCH, D_IF_AUTO_PATCH,
                "If ImmediatelyFast is installed on the client, automatically patch compatibility-safe config values in immediatelyfast.json.",
                "This only changes keys that are explicitly controlled by the options below.");
        s.bool(P_IF_DISABLE_SCREEN_BATCHING, D_IF_DISABLE_SCREEN_BATCHING,
                "Global override: force ImmediatelyFast 'experimental_screen_batching' off.",
                "Leave disabled unless you want this applied on all GPUs regardless of vendor.");
        s.bool(P_IF_DISABLE_SCREEN_BATCHING_INTEL_UHD, D_IF_DISABLE_SCREEN_BATCHING_INTEL_UHD,
                "If an Intel GPU is detected (UHD / Iris / Arc), force ImmediatelyFast 'experimental_screen_batching' off to mitigate known screen-freeze behavior.");
        s.bool(P_IF_DISABLE_FBO_SWITCHING_INTEL, D_IF_DISABLE_FBO_SWITCHING_INTEL,
                "If an Intel GPU is detected (UHD / Iris / Arc), force ImmediatelyFast 'avoid_redundant_framebuffer_switching' off.",
                "Targets Intel OpenGL driver instability reported with aggressive FBO optimization paths.");
        s.bool(P_IF_DISABLE_HUD_BATCHING, D_IF_DISABLE_HUD_BATCHING,
                "Optional stricter mode: also force ImmediatelyFast 'hud_batching' off.",
                "Leave disabled unless you still see compatibility issues after disabling screen batching.");
        s.bool(P_MLF_DISABLE_IF_SCREEN_BATCHING, D_MLF_DISABLE_IF_SCREEN_BATCHING,
                "If MemoryLeakFix and ImmediatelyFast are both installed, force ImmediatelyFast 'experimental_screen_batching' off.",
                "This preserves the compatibility safeguard implemented by Put A Plug In it without bundling MemoryLeakFix code.");

        s.bool(P_EMI_CLEAR_HISTORY, D_EMI_CLEAR_HISTORY,
                "EMI: clear the recipe-view history on respawn/dimension change so it stops retaining referenced item stacks.");
        s.bool(P_JADE_CLEAR_CACHES, D_JADE_CLEAR_CACHES,
                "Jade: clear the ObjectDataCenter accessor and the hideModName cache when the client level changes or you respawn.");
        s.bool(P_ICEBERG_CLEAR_CACHES, D_ICEBERG_CLEAR_CACHES,
                "Iceberg: on client world unload, clear the EntityCollector wrapped-level and entity caches and drop the",
                "CustomItemRenderer's cached wolf/horse/armor-stand/entity, which otherwise pin the old level.");
        s.bool(P_JEI_CLEAR_CACHES, D_JEI_CLEAR_CACHES,
                "JEI: on world unload and respawn, clear the RecipeTransferManager's unsupported-container set and drop",
                "the cached Grindstone menu.");
        s.bool(P_SUPPLEMENTARIES_CLEAR_CACHES, D_SUPPLEMENTARIES_CLEAR_CACHES,
                "Supplementaries: run its item/map/structure cache-clearing methods when the integrated server stops.");
        s.bool(P_SOPHCORE_CLEAR_WRAPPERS, D_SOPHCORE_CLEAR_WRAPPERS,
                "SophisticatedCore: on client world unload, invalidate the StorageWrapperRepository caches. They expire",
                "10 minutes after access, but Guava only evicts on a later cache operation, so leaving a world strands",
                "every storage wrapper (and its backing inventory) until you load another world.");
        s.bool(P_ETF_CLEAR_TEXTURES, D_ETF_CLEAR_TEXTURES,
                "Entity Texture Features: release the player-texture state held across a respawn/dimension change.",
                "On ETF 7+ the cached texture is re-pointed at the new player; on older ETF the map is cleared.");
        s.bool(P_ETF_CLEAR_RENDERER, D_ETF_CLEAR_RENDERER,
                "Entity Texture Features: after each living-entity render, drop the entity and player texture the",
                "renderer still holds. Runs per entity per frame; disable if you suspect a rendering conflict.");
        s.bool(P_EMF_CLEAR_RENDER_CONTEXT, D_EMF_CLEAR_RENDER_CONTEXT,
                "Entity Model Features: after each living-entity render, drop the animation iteration context the",
                "renderer still holds. No effect on EMF 3.0.6+, which no longer keeps it on the renderer.");
        s.bool(P_SOPHCORE_RESET_CRAFTING_UI, D_SOPHCORE_RESET_CRAFTING_UI,
                "SophisticatedCore: with CraftingTweaks installed, re-register the crafting UI part on world change",
                "so its singleton stops holding the storage screen, menu, player and level you left behind.");
        s.bool(P_MC_EXPIRE_DAMAGE_SOURCE, D_MC_EXPIRE_DAMAGE_SOURCE,
                "Minecraft: drop a living entity's expired last-damage source, which otherwise pins its attacker",
                "and that attacker's level until something reads it. Runs per living entity per tick.");
    }

    public static void loadAll(ConfigView v) {
        fixSophisticatedCoreItemStackKeyCache = v.getBool(P_SC_ENABLED, D_SC_ENABLED);
        scItemStackKeyCacheClientClearEveryTicks = v.getInt(P_SC_CLEAR_EVERY_TICKS, D_SC_CLEAR_EVERY_TICKS);
        scItemStackKeyCacheClientMinSizeBeforeClear = v.getInt(P_SC_MIN_SIZE, D_SC_MIN_SIZE);
        scItemStackKeyCacheClearOnServerTickEnd = v.getBool(P_SC_CLEAR_ON_SERVER, D_SC_CLEAR_ON_SERVER);

        leakCommandsEnabled = v.getBool(P_LEAK_COMMANDS, D_LEAK_COMMANDS);
        heapDumpCommandEnabled = v.getBool(P_HEAP_DUMP_COMMAND, D_HEAP_DUMP_COMMAND);

        clearModelManagerBlockStateToIdMapOnClientWorldLeave = v.getBool(P_MM_BLOCKSTATE_ON_LEAVE, D_MM_BLOCKSTATE_ON_LEAVE);
        clearModelManagerBakedModelMapsOnClientWorldLeave = v.getBool(P_MM_BAKED_ON_LEAVE, D_MM_BAKED_ON_LEAVE);
        clearModelManagerBakedModelMapsMinSize = v.getInt(P_MM_BAKED_MIN_SIZE, D_MM_BAKED_MIN_SIZE);

        compatibilityLayerEnabled = v.getBool(P_COMPAT_LAYER, D_COMPAT_LAYER);
        compatibilityLogDetectedMods = v.getBool(P_COMPAT_LOG_MODS, D_COMPAT_LOG_MODS);
        immediatelyFastCompatAutoPatchConfig = v.getBool(P_IF_AUTO_PATCH, D_IF_AUTO_PATCH);
        immediatelyFastCompatDisableScreenBatching = v.getBool(P_IF_DISABLE_SCREEN_BATCHING, D_IF_DISABLE_SCREEN_BATCHING);
        immediatelyFastCompatDisableScreenBatchingOnIntelUhd = v.getBool(P_IF_DISABLE_SCREEN_BATCHING_INTEL_UHD, D_IF_DISABLE_SCREEN_BATCHING_INTEL_UHD);
        immediatelyFastCompatDisableAvoidRedundantFramebufferSwitchingOnIntelGpu = v.getBool(P_IF_DISABLE_FBO_SWITCHING_INTEL, D_IF_DISABLE_FBO_SWITCHING_INTEL);
        immediatelyFastCompatDisableHudBatching = v.getBool(P_IF_DISABLE_HUD_BATCHING, D_IF_DISABLE_HUD_BATCHING);
        memoryLeakFixCompatDisableImmediatelyFastScreenBatching = v.getBool(P_MLF_DISABLE_IF_SCREEN_BATCHING, D_MLF_DISABLE_IF_SCREEN_BATCHING);

        emiClearHistoryOnRespawn = v.getBool(P_EMI_CLEAR_HISTORY, D_EMI_CLEAR_HISTORY);
        jadeClearCachesOnWorldChange = v.getBool(P_JADE_CLEAR_CACHES, D_JADE_CLEAR_CACHES);
        icebergClearCachesOnWorldUnload = v.getBool(P_ICEBERG_CLEAR_CACHES, D_ICEBERG_CLEAR_CACHES);
        jeiClearCachesOnWorldChange = v.getBool(P_JEI_CLEAR_CACHES, D_JEI_CLEAR_CACHES);
        supplementariesClearCachesOnServerStop = v.getBool(P_SUPPLEMENTARIES_CLEAR_CACHES, D_SUPPLEMENTARIES_CLEAR_CACHES);
        sophCoreClearStorageWrappersOnWorldUnload = v.getBool(P_SOPHCORE_CLEAR_WRAPPERS, D_SOPHCORE_CLEAR_WRAPPERS);
        etfClearPlayerTextureMapOnRespawn = v.getBool(P_ETF_CLEAR_TEXTURES, D_ETF_CLEAR_TEXTURES);
        etfClearRendererStateOnRenderEnd = v.getBool(P_ETF_CLEAR_RENDERER, D_ETF_CLEAR_RENDERER);
        emfClearRenderContextOnRenderEnd = v.getBool(P_EMF_CLEAR_RENDER_CONTEXT, D_EMF_CLEAR_RENDER_CONTEXT);
        sophCoreResetCraftingUiOnWorldChange = v.getBool(P_SOPHCORE_RESET_CRAFTING_UI, D_SOPHCORE_RESET_CRAFTING_UI);
        expireLastDamageSource = v.getBool(P_MC_EXPIRE_DAMAGE_SOURCE, D_MC_EXPIRE_DAMAGE_SOURCE);
    }

    public static boolean isFixSophisticatedCoreItemStackKeyCacheEnabled() {
        return fixSophisticatedCoreItemStackKeyCache;
    }

    public static int getScItemStackKeyCacheClientClearEveryTicks() {
        return scItemStackKeyCacheClientClearEveryTicks;
    }

    public static int getScItemStackKeyCacheClientMinSizeBeforeClear() {
        return scItemStackKeyCacheClientMinSizeBeforeClear;
    }

    public static boolean isScItemStackKeyCacheClearOnServerTickEnd() {
        return scItemStackKeyCacheClearOnServerTickEnd;
    }

    public static boolean isLeakCommandsEnabled() {
        return leakCommandsEnabled;
    }

    public static boolean isHeapDumpCommandEnabled() {
        return heapDumpCommandEnabled;
    }

    public static boolean isClearModelManagerBlockStateToIdMapOnClientWorldLeaveEnabled() {
        return clearModelManagerBlockStateToIdMapOnClientWorldLeave;
    }

    public static boolean isClearModelManagerBakedModelMapsOnClientWorldLeaveEnabled() {
        return clearModelManagerBakedModelMapsOnClientWorldLeave;
    }

    public static int getClearModelManagerBakedModelMapsMinSize() {
        return clearModelManagerBakedModelMapsMinSize;
    }

    public static boolean isCompatibilityLayerEnabled() {
        return compatibilityLayerEnabled;
    }

    public static boolean isCompatibilityLogDetectedModsEnabled() {
        return compatibilityLogDetectedMods;
    }

    public static boolean isImmediatelyFastCompatAutoPatchConfigEnabled() {
        return immediatelyFastCompatAutoPatchConfig;
    }

    public static boolean isImmediatelyFastCompatDisableScreenBatchingEnabled() {
        return immediatelyFastCompatDisableScreenBatching;
    }

    public static boolean isImmediatelyFastCompatDisableScreenBatchingOnIntelUhdEnabled() {
        return immediatelyFastCompatDisableScreenBatchingOnIntelUhd;
    }

    public static boolean isImmediatelyFastCompatDisableAvoidRedundantFramebufferSwitchingOnIntelGpuEnabled() {
        return immediatelyFastCompatDisableAvoidRedundantFramebufferSwitchingOnIntelGpu;
    }

    public static boolean isImmediatelyFastCompatDisableHudBatchingEnabled() {
        return immediatelyFastCompatDisableHudBatching;
    }

    public static boolean isMemoryLeakFixCompatDisableImmediatelyFastScreenBatchingEnabled() {
        return memoryLeakFixCompatDisableImmediatelyFastScreenBatching;
    }

    public static boolean isEmiClearHistoryOnRespawnEnabled() {
        return emiClearHistoryOnRespawn;
    }

    public static boolean isJadeClearCachesOnWorldChangeEnabled() {
        return jadeClearCachesOnWorldChange;
    }

    public static boolean isIcebergClearCachesOnWorldUnloadEnabled() {
        return icebergClearCachesOnWorldUnload;
    }

    public static boolean isJeiClearCachesOnWorldChangeEnabled() {
        return jeiClearCachesOnWorldChange;
    }

    public static boolean isSophCoreClearStorageWrappersOnWorldUnloadEnabled() {
        return sophCoreClearStorageWrappersOnWorldUnload;
    }

    public static boolean isSophCoreResetCraftingUiOnWorldChangeEnabled() {
        return sophCoreResetCraftingUiOnWorldChange;
    }

    public static boolean isExpireLastDamageSourceEnabled() {
        return expireLastDamageSource;
    }

    public static boolean isSupplementariesClearCachesOnServerStopEnabled() {
        return supplementariesClearCachesOnServerStop;
    }

    public static boolean isEtfClearPlayerTextureMapOnRespawnEnabled() {
        return etfClearPlayerTextureMapOnRespawn;
    }

    public static boolean isEtfClearRendererStateOnRenderEndEnabled() {
        return etfClearRendererStateOnRenderEnd;
    }

    public static boolean isEmfClearRenderContextOnRenderEndEnabled() {
        return emfClearRenderContextOnRenderEnd;
    }
}
