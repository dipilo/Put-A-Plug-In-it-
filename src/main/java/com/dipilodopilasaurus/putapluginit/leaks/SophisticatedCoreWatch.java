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

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.util.Map;

final class SophisticatedCoreWatch {
	private static final String SOPHISTICATED_CORE_MODID = "sophisticatedcore";
	private static final String ITEMSTACKKEY_CLASS = "net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey";
	private static final String ITEMSTACKKEY_CACHE_FIELD = "CACHE";

	private static volatile boolean triedInit = false;
	private static volatile boolean available = false;
	private static Field cacheField;

	private SophisticatedCoreWatch() {
	}

	@SuppressWarnings("java:S3011")
	private static void allowReflectiveAccess(Field field) {
		field.setAccessible(true);
	}

	static String tryGetItemStackKeyCacheSize() {
		initIfNeeded();
		if (!available || cacheField == null) {
			return "n/a";
		}
		try {
			Object cacheObj = cacheField.get(null);
			if (cacheObj instanceof Map<?, ?> map) {
				return Integer.toString(map.size());
			}
			return "unexpected(" + cacheObj.getClass().getName() + ")";
		} catch (Exception t) {
			available = false;
			return "error(" + t.getClass().getSimpleName() + ")";
		}
	}

	private static void initIfNeeded() {
		if (triedInit) {
			return;
		}
		synchronized (SophisticatedCoreWatch.class) {
			if (triedInit) {
				return;
			}
			triedInit = true;

			if (!ModList.get().isLoaded(SOPHISTICATED_CORE_MODID)) {
				available = false;
				return;
			}

			String detectedVersion = ModList.get().getModContainerById(SOPHISTICATED_CORE_MODID)
					.map(container -> container.getModInfo().getVersion().toString())
					.orElse("");
			if (!SophisticatedCoreVersionGate.shouldApplyItemStackKeyFix(detectedVersion)) {
				available = false;
				return;
			}

			try {
				Class<?> clazz = Class.forName(ITEMSTACKKEY_CLASS, false, SophisticatedCoreWatch.class.getClassLoader());
				Field f = clazz.getDeclaredField(ITEMSTACKKEY_CACHE_FIELD);
				allowReflectiveAccess(f);
				cacheField = f;
				available = true;
			} catch (Exception t) {
				available = false;
			}
		}
	}
}
