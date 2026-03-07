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
package com.dipilodopilasaurus.putapluginit.leaks;

import com.dipilodopilasaurus.putapluginit.Config;
import com.dipilodopilasaurus.putapluginit.PutAPlugInIt;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

@Mod.EventBusSubscriber(modid = PutAPlugInIt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SophisticatedCoreLeakFixes {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String SOPHISTICATED_CORE_MODID = "sophisticatedcore";
	private static final String ITEMSTACKKEY_CLASS = "net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey";
	private static final String ITEMSTACKKEY_CACHE_FIELD = "CACHE";

	private static volatile boolean triedInit = false;
	private static volatile boolean available = false;
	private static Field itemStackKeyCacheField;
	private static int clientTickCounter = 0;

	private SophisticatedCoreLeakFixes() {
	}

	@SuppressWarnings("java:S3011")
	private static void allowReflectiveAccess(Field field) {
		field.setAccessible(true);
	}

	private static void initIfNeeded() {
		if (triedInit) {
			return;
		}
		synchronized (SophisticatedCoreLeakFixes.class) {
			if (triedInit) {
				return;
			}
			triedInit = true;

			if (!ModList.get().isLoaded(SOPHISTICATED_CORE_MODID)) {
				available = false;
				return;
			}

			String detectedSophisticatedCoreVersion = ModList.get().getModContainerById(SOPHISTICATED_CORE_MODID)
					.map(container -> container.getModInfo().getVersion().toString())
					.orElse("");
			if (!SophisticatedCoreVersionGate.shouldApplyItemStackKeyFix(detectedSophisticatedCoreVersion)) {
				available = false;
				LOGGER.info("[papi] SophisticatedCore {} already contains native ItemStackKey cache fix; disabling papi duplicate mitigation.", detectedSophisticatedCoreVersion);
				return;
			}

			try {
				Class<?> clazz = Class.forName(ITEMSTACKKEY_CLASS, false, SophisticatedCoreLeakFixes.class.getClassLoader());
				Field cacheField = clazz.getDeclaredField(ITEMSTACKKEY_CACHE_FIELD);
				allowReflectiveAccess(cacheField);
				itemStackKeyCacheField = cacheField;
				available = true;
				LOGGER.info("[papi] Enabled SophisticatedCore leak fix for {}.{}", ITEMSTACKKEY_CLASS, ITEMSTACKKEY_CACHE_FIELD);
			} catch (Exception t) {
				available = false;
				LOGGER.warn("[papi] Failed to enable SophisticatedCore leak fix for {}.{}; will do nothing.", ITEMSTACKKEY_CLASS, ITEMSTACKKEY_CACHE_FIELD, t);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void clearItemStackKeyCache() {
		initIfNeeded();
		if (!available || itemStackKeyCacheField == null) {
			return;
		}

		try {
			Object cacheObj = itemStackKeyCacheField.get(null);
			if (cacheObj instanceof Map<?, ?> map) {
				((Map<Object, Object>) map).clear();
			}
		} catch (Exception t) {
			// If something goes wrong at runtime, disable to avoid spamming logs
			available = false;
			LOGGER.warn("[papi] Disabling SophisticatedCore ItemStackKey cache leak fix due to runtime error.", t);
		}
	}

	@SuppressWarnings("unchecked")
	private static int tryGetItemStackKeyCacheSize() {
		initIfNeeded();
		if (!available || itemStackKeyCacheField == null) {
			return -1;
		}
		try {
			Object cacheObj = itemStackKeyCacheField.get(null);
			if (cacheObj instanceof Map<?, ?> map) {
				return ((Map<Object, Object>) map).size();
			}
			return -1;
		} catch (Exception t) {
			available = false;
			return -1;
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (!Config.isFixSophisticatedCoreItemStackKeyCacheEnabled()) {
			return;
		}

		int every = Config.getScItemStackKeyCacheClientClearEveryTicks();
		if (every <= 0) {
			return;
		}
		clientTickCounter++;
		if ((clientTickCounter % every) != 0) {
			return;
		}

		int minSize = Math.max(0, Config.getScItemStackKeyCacheClientMinSizeBeforeClear());
		int size = tryGetItemStackKeyCacheSize();
		if (size >= 0 && size < minSize) {
			return;
		}
		clearItemStackKeyCache();
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (!Config.isFixSophisticatedCoreItemStackKeyCacheEnabled()) {
			return;
		}
		if (!Config.isScItemStackKeyCacheClearOnServerTickEnd()) {
			return;
		}
		clearItemStackKeyCache();
	}
}
