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

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = PutAPlugInIt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        private Config() {
        }

    private static final ForgeConfigSpec.BooleanValue FIX_SC_ITEMSTACKKEY_CACHE = BUILDER
                        .comment("Leak fix: clear SophisticatedCore ItemStackKey cache periodically on the client to prevent unbounded growth.",
                                        "SophisticatedCore already clears this cache every server tick; clearing it on the client is the primary mitigation.")
                        .define("leakFixes.sophisticatedCore.itemStackKeyCache.enabled", true);

        private static final ForgeConfigSpec.IntValue FIX_SC_ITEMSTACKKEY_CACHE_CLEAR_EVERY_TICKS = BUILDER
                        .comment("How often to clear SophisticatedCore ItemStackKey cache on the CLIENT (in ticks).",
                                        "Set to 1 to clear every tick (most aggressive, may cause UI lag).")
                        .defineInRange("leakFixes.sophisticatedCore.itemStackKeyCache.client.clearEveryTicks", 20, 1, 20_000);

        private static final ForgeConfigSpec.IntValue FIX_SC_ITEMSTACKKEY_CACHE_MIN_SIZE = BUILDER
                        .comment("Only clear the SophisticatedCore ItemStackKey cache if it has at least this many entries.")
                        .defineInRange("leakFixes.sophisticatedCore.itemStackKeyCache.client.minSizeBeforeClear", 10_000, 0, 5_000_000);

        private static final ForgeConfigSpec.BooleanValue FIX_SC_ITEMSTACKKEY_CACHE_CLEAR_ON_SERVER = BUILDER
                        .comment("Also clear SophisticatedCore ItemStackKey cache on the SERVER tick end.",
                                        "Not usually needed (SophisticatedCore already clears it every server tick).",
                                        "Enable only for debugging or if SophisticatedCore changes behavior.")
                        .define("leakFixes.sophisticatedCore.itemStackKeyCache.server.clearOnTickEnd", false);

    private static final ForgeConfigSpec.BooleanValue LEAK_COMMANDS_ENABLED = BUILDER
            .comment("Enable /papi leak commands for quick memory/leak diagnostics.")
            .define("leakTools.commands.enabled", true);

    private static final ForgeConfigSpec.BooleanValue HEAP_DUMP_COMMAND_ENABLED = BUILDER
            .comment("Enable /papi leak dumpHeap (writes a .hprof). Disabled by default because it can freeze the game and produce huge files.")
            .define("leakTools.commands.heapDump.enabled", false);

        private static final ForgeConfigSpec.BooleanValue CLEAR_MM_BLOCKSTATE_MAP_ON_WORLD_LEAVE = BUILDER
                        .comment("Mitigation: when leaving a client world (back to menu), attempt to clear the ModelManager BlockState->int cache if detected.",
                                        "This is an opt-in, best-effort reflection-based cleanup. It should be safe, but may be ineffective if the MAT suspects are not caused by this cache.")
                        .define("leakFixes.minecraft.modelManager.blockStateToIdMap.clearOnClientWorldLeave", false);

        private static final ForgeConfigSpec.BooleanValue CLEAR_MM_BAKED_MODEL_MAPS_ON_WORLD_LEAVE = BUILDER
                        .comment("MemoryLeakFix-derived mitigation: when leaving a client world, attempt to clear large ModelManager baked-model maps.",
                                        "This targets stale client-side model caches that may retain many entries across world transitions.")
                        .define("leakFixes.minecraft.modelManager.bakedModelMaps.clearOnClientWorldLeave", true);

        private static final ForgeConfigSpec.IntValue CLEAR_MM_BAKED_MODEL_MAPS_MIN_SIZE = BUILDER
                        .comment("Only clear ModelManager baked-model maps that have at least this many entries.")
                        .defineInRange("leakFixes.minecraft.modelManager.bakedModelMaps.client.minSizeBeforeClear", 10_000, 0, 5_000_000);

            private static final ForgeConfigSpec.BooleanValue COMPAT_LAYER_ENABLED = BUILDER
                    .comment("Enable Put A Plug In it's compatibility layer for interoperability with other optimization mods.")
                    .define("compatibility.layer.enabled", true);

            private static final ForgeConfigSpec.BooleanValue COMPAT_LOG_DETECTED_MODS = BUILDER
                    .comment("Log detected compatibility targets (ImmediatelyFast) at startup.")
                    .define("compatibility.layer.logDetectedMods", true);

            private static final ForgeConfigSpec.BooleanValue IF_COMPAT_AUTO_PATCH_CONFIG = BUILDER
                    .comment("If ImmediatelyFast is installed on the client, automatically patch compatibility-safe config values in immediatelyfast.json.",
                            "This only changes keys that are explicitly controlled by the options below.")
                    .define("compatibility.immediatelyFast.autoPatchConfig", true);

            private static final ForgeConfigSpec.BooleanValue IF_COMPAT_DISABLE_SCREEN_BATCHING = BUILDER
                    .comment("Global override: force ImmediatelyFast 'experimental_screen_batching' off.",
                            "Leave disabled unless you want this applied on all GPUs regardless of vendor.")
                    .define("compatibility.immediatelyFast.disableScreenBatching", false);

            private static final ForgeConfigSpec.BooleanValue IF_COMPAT_DISABLE_SCREEN_BATCHING_ON_INTEL_UHD = BUILDER
                    .comment("If an Intel GPU is detected (UHD / Iris / Arc), force ImmediatelyFast 'experimental_screen_batching' off to mitigate known screen-freeze behavior.")
                    .define("compatibility.immediatelyFast.disableScreenBatchingOnIntelUhd", true);

            private static final ForgeConfigSpec.BooleanValue IF_COMPAT_DISABLE_REDUNDANT_FBO_SWITCHING_ON_INTEL = BUILDER
                    .comment("If an Intel GPU is detected (UHD / Iris / Arc), force ImmediatelyFast 'avoid_redundant_framebuffer_switching' off.",
                            "Targets Intel OpenGL driver instability reported with aggressive FBO optimization paths.")
                    .define("compatibility.immediatelyFast.disableAvoidRedundantFramebufferSwitchingOnIntelGpu", true);

            private static final ForgeConfigSpec.BooleanValue IF_COMPAT_DISABLE_HUD_BATCHING = BUILDER
                    .comment("Optional stricter mode: also force ImmediatelyFast 'hud_batching' off.",
                            "Leave disabled unless you still see compatibility issues after disabling screen batching.")
                    .define("compatibility.immediatelyFast.disableHudBatching", false);

            private static final ForgeConfigSpec.BooleanValue MLF_COMPAT_DISABLE_IF_SCREEN_BATCHING = BUILDER
                    .comment("If MemoryLeakFix and ImmediatelyFast are both installed, force ImmediatelyFast 'experimental_screen_batching' off.",
                            "This preserves the compatibility safeguard implemented by Put A Plug In it without bundling MemoryLeakFix code.")
                    .define("compatibility.memoryLeakFix.disableImmediatelyFastScreenBatching", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean fixSophisticatedCoreItemStackKeyCache;
    private static volatile int scItemStackKeyCacheClientClearEveryTicks;
    private static volatile int scItemStackKeyCacheClientMinSizeBeforeClear;
    private static volatile boolean scItemStackKeyCacheClearOnServerTickEnd;

    private static volatile boolean leakCommandsEnabled;
    private static volatile boolean heapDumpCommandEnabled;
    private static volatile boolean clearModelManagerBlockStateToIdMapOnClientWorldLeave;
        private static volatile boolean clearModelManagerBakedModelMapsOnClientWorldLeave;
        private static volatile int clearModelManagerBakedModelMapsMinSize;
    private static volatile boolean compatibilityLayerEnabled = true;
    private static volatile boolean compatibilityLogDetectedMods = true;
    private static volatile boolean immediatelyFastCompatAutoPatchConfig = true;
    private static volatile boolean immediatelyFastCompatDisableScreenBatching = true;
    private static volatile boolean immediatelyFastCompatDisableScreenBatchingOnIntelUhd = true;
        private static volatile boolean immediatelyFastCompatDisableAvoidRedundantFramebufferSwitchingOnIntelGpu = true;
    private static volatile boolean immediatelyFastCompatDisableHudBatching = false;
        private static volatile boolean memoryLeakFixCompatDisableImmediatelyFastScreenBatching = true;

    @SubscribeEvent
        static void onLoad(final ModConfigEvent event) {
                fixSophisticatedCoreItemStackKeyCache = FIX_SC_ITEMSTACKKEY_CACHE.get();
                scItemStackKeyCacheClientClearEveryTicks = FIX_SC_ITEMSTACKKEY_CACHE_CLEAR_EVERY_TICKS.get();
                scItemStackKeyCacheClientMinSizeBeforeClear = FIX_SC_ITEMSTACKKEY_CACHE_MIN_SIZE.get();
                scItemStackKeyCacheClearOnServerTickEnd = FIX_SC_ITEMSTACKKEY_CACHE_CLEAR_ON_SERVER.get();

                leakCommandsEnabled = LEAK_COMMANDS_ENABLED.get();
                heapDumpCommandEnabled = HEAP_DUMP_COMMAND_ENABLED.get();
                clearModelManagerBlockStateToIdMapOnClientWorldLeave = CLEAR_MM_BLOCKSTATE_MAP_ON_WORLD_LEAVE.get();
                clearModelManagerBakedModelMapsOnClientWorldLeave = CLEAR_MM_BAKED_MODEL_MAPS_ON_WORLD_LEAVE.get();
                clearModelManagerBakedModelMapsMinSize = CLEAR_MM_BAKED_MODEL_MAPS_MIN_SIZE.get();

                compatibilityLayerEnabled = COMPAT_LAYER_ENABLED.get();
                compatibilityLogDetectedMods = COMPAT_LOG_DETECTED_MODS.get();
                immediatelyFastCompatAutoPatchConfig = IF_COMPAT_AUTO_PATCH_CONFIG.get();
                immediatelyFastCompatDisableScreenBatching = IF_COMPAT_DISABLE_SCREEN_BATCHING.get();
                immediatelyFastCompatDisableScreenBatchingOnIntelUhd = IF_COMPAT_DISABLE_SCREEN_BATCHING_ON_INTEL_UHD.get();
                immediatelyFastCompatDisableAvoidRedundantFramebufferSwitchingOnIntelGpu = IF_COMPAT_DISABLE_REDUNDANT_FBO_SWITCHING_ON_INTEL.get();
                immediatelyFastCompatDisableHudBatching = IF_COMPAT_DISABLE_HUD_BATCHING.get();
                memoryLeakFixCompatDisableImmediatelyFastScreenBatching = MLF_COMPAT_DISABLE_IF_SCREEN_BATCHING.get();
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
}
