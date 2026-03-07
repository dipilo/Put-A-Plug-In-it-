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
package com.dipilodopilasaurus.putapluginit.compat;

import com.dipilodopilasaurus.putapluginit.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.opengl.GL11C;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ImmediatelyFastCompatClient {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String EXPERIMENTAL_SCREEN_BATCHING_KEY = "experimental_screen_batching";
	private static final String HUD_BATCHING_KEY = "hud_batching";
	private static final String AVOID_REDUNDANT_FRAMEBUFFER_SWITCHING_KEY = "avoid_redundant_framebuffer_switching";

	private static boolean registered = false;
	private static boolean patched = false;

	private ImmediatelyFastCompatClient() {
	}

	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		MinecraftForge.EVENT_BUS.register(ImmediatelyFastCompatClient.class);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || patched) {
			return;
		}
		patched = true;
		applyCompatibilityPatch();
	}

	private static void applyCompatibilityPatch() {
		if (!Config.isCompatibilityLayerEnabled() || !Config.isImmediatelyFastCompatAutoPatchConfigEnabled()) {
			return;
		}
		if (!ModList.get().isLoaded(ModCompatibilityLayer.IMMEDIATELY_FAST_MODID)) {
			return;
		}

		Path ifConfigPath = FMLPaths.CONFIGDIR.get().resolve("immediatelyfast.json");
		if (!Files.exists(ifConfigPath)) {
			LOGGER.info("[papi] ImmediatelyFast config not found at {} (nothing to patch yet).", ifConfigPath);
			return;
		}

		try {
			JsonObject root = readJsonObject(ifConfigPath);
			if (root == null) {
				LOGGER.warn("[papi] Could not parse {} as a JSON object. Skipping compatibility patch.", ifConfigPath);
				return;
			}

			boolean changed = false;
			boolean intelGpu = isIntelGpu();
			boolean hasMemoryLeakFix = ModList.get().isLoaded(ModCompatibilityLayer.MEMORY_LEAK_FIX_MODID);

			if (Config.isImmediatelyFastCompatDisableScreenBatchingOnIntelUhdEnabled() && intelGpu) {
				changed |= setBoolean(root, EXPERIMENTAL_SCREEN_BATCHING_KEY, false);
			}

			if (Config.isImmediatelyFastCompatDisableAvoidRedundantFramebufferSwitchingOnIntelGpuEnabled() && intelGpu) {
				changed |= setBoolean(root, AVOID_REDUNDANT_FRAMEBUFFER_SWITCHING_KEY, false);
			}

			if (Config.isImmediatelyFastCompatDisableScreenBatchingEnabled()) {
				changed |= setBoolean(root, EXPERIMENTAL_SCREEN_BATCHING_KEY, false);
			}

			if (Config.isImmediatelyFastCompatDisableHudBatchingEnabled()) {
				changed |= setBoolean(root, HUD_BATCHING_KEY, false);
			}

			if (hasMemoryLeakFix && Config.isMemoryLeakFixCompatDisableImmediatelyFastScreenBatchingEnabled()) {
				changed |= setBoolean(root, EXPERIMENTAL_SCREEN_BATCHING_KEY, false);
			}

			if (!changed) {
				return;
			}

			writeJsonObject(ifConfigPath, root);
			LOGGER.warn("[papi] Patched ImmediatelyFast compatibility settings in {}. Restart Minecraft for changes to take effect.", ifConfigPath);
		} catch (IOException | RuntimeException t) {
			LOGGER.warn("[papi] Failed to patch ImmediatelyFast compatibility settings.", t);
		}
	}

	private static JsonObject readJsonObject(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonElement element = JsonParser.parseReader(reader);
			if (!element.isJsonObject()) {
				return null;
			}
			return element.getAsJsonObject();
		}
	}

	private static void writeJsonObject(Path path, JsonObject jsonObject) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(jsonObject, writer);
		}
	}

	private static boolean setBoolean(JsonObject root, String key, boolean value) {
		if (root.has(key) && root.get(key).isJsonPrimitive() && root.get(key).getAsBoolean() == value) {
			return false;
		}
		root.addProperty(key, value);
		return true;
	}

	private static boolean isIntelGpu() {
		try {
			String vendor = GL11C.glGetString(GL11C.GL_VENDOR);
			String renderer = GL11C.glGetString(GL11C.GL_RENDERER);
			if (vendor == null && renderer == null) {
				return false;
			}

			String vendorLower = vendor == null ? "" : vendor.toLowerCase();
			String rendererLower = renderer == null ? "" : renderer.toLowerCase();
			return vendorLower.contains("intel") || rendererLower.contains("intel") || rendererLower.contains("iris") || rendererLower.contains("arc");
		} catch (RuntimeException t) {
			return false;
		}
	}
}
