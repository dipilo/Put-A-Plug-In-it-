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

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;

final class MinecraftClientMitigations {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String MC_MINECRAFT = "net.minecraft.client.Minecraft";
	private static final String MC_CLIENT_LEVEL = "net.minecraft.client.multiplayer.ClientLevel";
	private static final String MC_MODEL_MANAGER = "net.minecraft.client.resources.model.ModelManager";
	private static final String MC_BLOCK_STATE = "net.minecraft.world.level.block.state.BlockState";
	private static final String MC_BAKED_MODEL = "net.minecraft.client.resources.model.BakedModel";
	private static final String FASTUTIL_OBJECT2INT_MAP_CLASS = "it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap";

	private MinecraftClientMitigations() {
	}

	static boolean tryHasClientWorld() {
		try {
			Object minecraft = getMinecraftInstance();
			if (minecraft == null) {
				return false;
			}

			Object clientLevel = getFirstFieldByTypeName(minecraft, MC_CLIENT_LEVEL);
			return clientLevel != null;
		} catch (Exception t) {
			return false;
		}
	}

	/**
	 * Best-effort: locate the ModelManager BlockState->int Object2IntOpenHashMap and clear it.
	 * Runs only on the client, via reflection, and should be used when no world is loaded.
	 */
	static ClearResult tryClearModelManagerBlockStateToIntMap() {
		try {
			Object minecraft = getMinecraftInstance();
			if (minecraft == null) {
				return ClearResult.notClient();
			}

			Object modelManager = getFirstFieldByTypeName(minecraft, MC_MODEL_MANAGER);
			if (modelManager == null) {
				return ClearResult.failure("missing ModelManager");
			}

			Object map = findBlockStateToIntMap(modelManager);
			if (map == null) {
				return ClearResult.failure("BlockState->int map not found");
			}

			int before = (int) map.getClass().getMethod("size").invoke(map);
			map.getClass().getMethod("clear").invoke(map);
			int after = (int) map.getClass().getMethod("size").invoke(map);
			LOGGER.info("[papi] Cleared ModelManager BlockState->int map ({} -> {})", before, after);
			return ClearResult.cleared(before, after);
		} catch (Exception t) {
			return ClearResult.failure(t.getClass().getSimpleName() + ": " + safeMsg(t.getMessage()));
		}
	}

	/**
	 * MemoryLeakFix-style cleanup: clear large ModelManager maps whose values are BakedModel instances.
	 */
	static BakedModelMapClearResult tryClearLargeBakedModelMaps(int minSize) {
		try {
			Object minecraft = getMinecraftInstance();
			if (minecraft == null) {
				return BakedModelMapClearResult.notClient();
			}

			Object modelManager = getFirstFieldByTypeName(minecraft, MC_MODEL_MANAGER);
			if (modelManager == null) {
				return BakedModelMapClearResult.failure("missing ModelManager");
			}

			int clearedMaps = 0;
			int clearedEntries = 0;
			for (Field field : modelManager.getClass().getDeclaredFields()) {
				Object value = readFieldValue(modelManager, field);
				if (value instanceof java.util.Map<?, ?> map) {
					int size = map.size();
					if (size >= minSize && hasBakedModelValues(map)) {
						map.clear();
						clearedMaps++;
						clearedEntries += size;
					}
				}
			}

			return BakedModelMapClearResult.cleared(clearedMaps, clearedEntries);
		} catch (Exception t) {
			return BakedModelMapClearResult.failure(t.getClass().getSimpleName() + ": " + safeMsg(t.getMessage()));
		}
	}

	private static Object getMinecraftInstance() throws ReflectiveOperationException {
		try {
			Class<?> mcClazz = Class.forName(MC_MINECRAFT, false, MinecraftClientMitigations.class.getClassLoader());
			return mcClazz.getMethod("getInstance").invoke(null);
		} catch (ClassNotFoundException e) {
			return null; // dedicated server
		}
	}

	private static Object getFirstFieldByTypeName(Object instance, String typeName) {
		for (Field f : instance.getClass().getDeclaredFields()) {
			if (!f.getType().getName().equals(typeName)) {
				continue;
			}
			try {
				allowReflectiveAccess(f);
				return f.get(instance);
			} catch (Exception ignored) {
				return null;
			}
		}
		return null;
	}

	@SuppressWarnings("java:S3011")
	private static void allowReflectiveAccess(Field field) {
		field.trySetAccessible();
	}

	private static Object findBlockStateToIntMap(Object modelManager) {
		for (Field f : modelManager.getClass().getDeclaredFields()) {
			Object value = readFieldValue(modelManager, f);
			if (isBlockStateKeyedObject2IntMap(value)) {
				return value;
			}
		}
		return null;
	}

	private static Object readFieldValue(Object owner, Field field) {
		try {
			allowReflectiveAccess(field);
			return field.get(owner);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static boolean isBlockStateKeyedObject2IntMap(Object value) {
		if (value == null) {
			return false;
		}
		if (!isInstanceOf(value, FASTUTIL_OBJECT2INT_MAP_CLASS)) {
			return false;
		}
		try {
			Object keySet = value.getClass().getMethod("keySet").invoke(value);
			Object iterator = keySet.getClass().getMethod("iterator").invoke(keySet);
			boolean hasNext = (boolean) iterator.getClass().getMethod("hasNext").invoke(iterator);
			if (!hasNext) {
				return false;
			}
			Object firstKey = iterator.getClass().getMethod("next").invoke(iterator);
			return firstKey != null && MC_BLOCK_STATE.equals(firstKey.getClass().getName());
		} catch (Exception ignored) {
			return false;
		}
	}

	private static boolean hasBakedModelValues(java.util.Map<?, ?> map) {
		for (Object value : map.values()) {
			if (value != null) {
				return isInstanceOf(value, MC_BAKED_MODEL);
			}
		}
		return false;
	}

	private static boolean isInstanceOf(Object value, String className) {
		try {
			Class<?> clazz = Class.forName(className, false, MinecraftClientMitigations.class.getClassLoader());
			return clazz.isInstance(value);
		} catch (ClassNotFoundException ignored) {
			return false;
		}
	}

	private static String safeMsg(String msg) {
		return msg == null ? "" : msg;
	}

	record ClearResult(boolean isClient, boolean cleared, Integer before, Integer after, String message) {
		static ClearResult notClient() {
			return new ClearResult(false, false, null, null, "not client");
		}

		static ClearResult cleared(int before, int after) {
			return new ClearResult(true, true, before, after, null);
		}

		static ClearResult failure(String message) {
			return new ClearResult(true, false, null, null, message);
		}
	}

	record BakedModelMapClearResult(boolean isClient, int clearedMaps, int clearedEntries, String message) {
		static BakedModelMapClearResult notClient() {
			return new BakedModelMapClearResult(false, 0, 0, "not client");
		}

		static BakedModelMapClearResult cleared(int maps, int entries) {
			return new BakedModelMapClearResult(true, maps, entries, null);
		}

		static BakedModelMapClearResult failure(String message) {
			return new BakedModelMapClearResult(true, 0, 0, message);
		}
	}
}
