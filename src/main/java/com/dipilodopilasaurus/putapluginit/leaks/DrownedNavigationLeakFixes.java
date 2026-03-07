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

import com.dipilodopilasaurus.putapluginit.PutAPlugInIt;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = PutAPlugInIt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DrownedNavigationLeakFixes {
	private static final String MLF_MODID = "memoryleakfix";
	private static volatile boolean enabledResolved = false;
	private static volatile boolean enabled = false;

	private DrownedNavigationLeakFixes() {
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (!isEnabled()) {
			return;
		}
		Object entity = event.getEntity();
		if (entity == null || !entity.getClass().getName().contains("Drowned")) {
			return;
		}
		Object level = event.getLevel();
		if (level == null) {
			return;
		}
		Set<Object> navigations = readNavigationSet(level);
		if (navigations.isEmpty()) {
			return;
		}
		for (Object navigation : collectNavigationFields(entity)) {
			navigations.remove(navigation);
		}
	}

	private static boolean isEnabled() {
		if (enabledResolved) {
			return enabled;
		}
		enabledResolved = true;
		enabled = isRange1163To1165() && !isModLoaded(MLF_MODID);
		return enabled;
	}

	private static boolean isRange1163To1165() {
		String version = getMinecraftVersionName();
		return compare(version, "1.16.3") >= 0 && compare(version, "1.16.5") <= 0;
	}

	private static String getMinecraftVersionName() {
		try {
			Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants");
			Object current = sharedConstantsClass.getMethod("getCurrentVersion").invoke(null);
			Object name = current.getClass().getMethod("getName").invoke(current);
			return name == null ? "" : name.toString();
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return "";
		}
	}

	private static int compare(String left, String right) {
		String[] l = sanitize(left).split("\\.");
		String[] r = sanitize(right).split("\\.");
		int len = Math.max(l.length, r.length);
		for (int i = 0; i < len; i++) {
			int li = i < l.length ? parse(l[i]) : 0;
			int ri = i < r.length ? parse(r[i]) : 0;
			if (li != ri) {
				return Integer.compare(li, ri);
			}
		}
		return 0;
	}

	private static String sanitize(String value) {
		if (value == null) {
			return "0";
		}
		String base = value.trim();
		int idx = base.indexOf(' ');
		if (idx >= 0) {
			base = base.substring(0, idx);
		}
		return base.replaceAll("[^0-9.]", "");
	}

	private static int parse(String value) {
		if (value == null || value.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	private static boolean isModLoaded(String modId) {
		return isForgeLikeModLoaded("net.minecraftforge.fml.ModList", modId)
				|| isForgeLikeModLoaded("net.neoforged.fml.ModList", modId)
				|| isFabricModLoaded(modId);
	}

	private static boolean isForgeLikeModLoaded(String modListClassName, String modId) {
		try {
			Class<?> modListClass = Class.forName(modListClassName, false, DrownedNavigationLeakFixes.class.getClassLoader());
			Object modList = modListClass.getMethod("get").invoke(null);
			return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	private static boolean isFabricModLoaded(String modId) {
		try {
			Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, DrownedNavigationLeakFixes.class.getClassLoader());
			Object loader = loaderClass.getMethod("getInstance").invoke(null);
			return (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	private static Set<Object> readNavigationSet(Object level) {
		Class<?> cursor = level.getClass();
		while (cursor != null && cursor != Object.class) {
			for (Field field : cursor.getDeclaredFields()) {
				Set<Object> candidate = readNavigationSetField(level, field);
				if (!candidate.isEmpty()) {
					return candidate;
				}
			}
			cursor = cursor.getSuperclass();
		}
		return Collections.emptySet();
	}

	@SuppressWarnings({"unchecked", "java:S3011"})
	private static Set<Object> readNavigationSetField(Object level, Field field) {
		if (!isNavigationSetField(field)) {
			return Collections.emptySet();
		}
		try {
			field.setAccessible(true);
			Object value = field.get(level);
			if (value instanceof Set<?> set) {
				return (Set<Object>) set;
			}
		} catch (IllegalAccessException | RuntimeException ignored) {
			return Collections.emptySet();
		}
		return Collections.emptySet();
	}

	private static boolean isNavigationSetField(Field field) {
		return Set.class.isAssignableFrom(field.getType())
				&& field.getName().toLowerCase().contains("navigation");
	}

	private static Set<Object> collectNavigationFields(Object entity) {
		Set<Object> result = new HashSet<>();
		Class<?> cursor = entity.getClass();
		while (cursor != null && cursor != Object.class) {
			for (Field field : cursor.getDeclaredFields()) {
				addNavigationFieldValue(entity, field, result);
			}
			cursor = cursor.getSuperclass();
		}
		return result;
	}

	@SuppressWarnings("java:S3011")
	private static void addNavigationFieldValue(Object entity, Field field, Set<Object> result) {
		if (!field.getType().getName().contains("PathNavigation")) {
			return;
		}
		try {
			field.setAccessible(true);
			Object value = field.get(entity);
			if (value != null) {
				result.add(value);
			}
		} catch (IllegalAccessException | RuntimeException ignored) {
			// Best-effort; continue scanning
		}
	}
}
