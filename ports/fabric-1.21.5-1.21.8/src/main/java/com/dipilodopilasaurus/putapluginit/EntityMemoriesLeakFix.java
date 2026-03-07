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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public final class EntityMemoriesLeakFix {
    private static final String MLF_MODID = "memoryleakfix";

    private static boolean checkedEnabled = false;
    private static boolean enabled = false;

    private EntityMemoriesLeakFix() {
    }

    public static void onEntityRemoved(Object entity) {
        if (!isEnabled() || !isLivingEntity(entity)) {
            return;
        }
        clearLivingEntityBrainMemories(entity);
    }

    private static boolean isEnabled() {
        if (checkedEnabled) {
            return enabled;
        }
        checkedEnabled = true;
        enabled = isAffectedMinecraftVersion() && !isModLoaded(MLF_MODID);
        return enabled;
    }

    private static boolean isAffectedMinecraftVersion() {
        try {
            String versionName = resolveMinecraftVersionName();
            return compareSimpleVersion(versionName, "1.20.0") < 0;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String resolveMinecraftVersionName() {
        try {
            Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants", false, EntityMemoriesLeakFix.class.getClassLoader());
            Object worldVersion = invokeFirstNoArgMethod(sharedConstantsClass, "getCurrentVersion", "getGameVersion", "createGameVersion");
            return extractVersionName(worldVersion);
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static Object invokeFirstNoArgMethod(Class<?> owner, String... methodNames) throws ReflectiveOperationException {
        for (String methodName : methodNames) {
            try {
                Method method = owner.getMethod(methodName);
                return method.invoke(null);
            } catch (NoSuchMethodException ignored) {
                // Try next candidate.
            }
        }
        throw new NoSuchMethodException("No matching SharedConstants version accessor found");
    }

    private static String extractVersionName(Object worldVersion) {
        if (worldVersion == null) {
            return "";
        }
        try {
            Method getName = worldVersion.getClass().getMethod("getName");
            Object value = getName.invoke(worldVersion);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            try {
                Method getId = worldVersion.getClass().getMethod("getId");
                Object value = getId.invoke(worldVersion);
                return value == null ? "" : value.toString();
            } catch (ReflectiveOperationException ignoredAgain) {
                return worldVersion.toString();
            }
        }
    }

    private static int compareSimpleVersion(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int len = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < len; i++) {
            int li = i < leftParts.length ? parseIntPrefix(leftParts[i]) : 0;
            int ri = i < rightParts.length ? parseIntPrefix(rightParts[i]) : 0;
            if (li != ri) {
                return Integer.compare(li, ri);
            }
        }
        return 0;
    }

    private static int parseIntPrefix(String value) {
        String digits = value.replaceAll("\\D.*$", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean isModLoaded(String modId) {
        if (isForgeLikeModLoaded("net.minecraftforge.fml.ModList", modId)) {
            return true;
        }
        if (isForgeLikeModLoaded("net.neoforged.fml.ModList", modId)) {
            return true;
        }
        return isFabricModLoaded(modId);
    }

    private static boolean isForgeLikeModLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, EntityMemoriesLeakFix.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, EntityMemoriesLeakFix.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "java:S3011"})
    private static void clearLivingEntityBrainMemories(Object livingEntity) {
        Object brain = getBrain(livingEntity);
        if (brain == null || !hasBrainType(brain)) {
            return;
        }

        Field memoriesField = findMemoriesField(brain.getClass());
        if (memoriesField == null) {
            return;
        }

        try {
            memoriesField.setAccessible(true);
            Object memoriesObj = memoriesField.get(brain);
            if (!(memoriesObj instanceof Map<?, ?>)) {
                return;
            }
            Map<?, ?> memoriesMap = (Map<?, ?>) memoriesObj;

            Method setMemoryMethod = findSetMemoryMethod(brain.getClass());
            if (setMemoryMethod == null) {
                return;
            }

            for (Object key : new ArrayList<>(memoriesMap.keySet())) {
                invokeSetMemoryBestEffort(setMemoryMethod, brain, key);
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
            // Best-effort cleanup; if internals differ in a patch, skip silently.
        }
    }

	private static void invokeSetMemoryBestEffort(Method setMemoryMethod, Object brain, Object key) {
		try {
			setMemoryMethod.invoke(brain, key, Optional.empty());
		} catch (ReflectiveOperationException ignored) {
			// Best-effort cleanup for each memory entry.
		}
	}

    private static boolean isLivingEntity(Object entity) {
        if (entity == null) {
            return false;
        }
        String className = entity.getClass().getName();
        return "net.minecraft.world.entity.LivingEntity".equals(className)
                || "net.minecraft.entity.LivingEntity".equals(className)
                || isSubclassNamed(entity.getClass(), "net.minecraft.world.entity.LivingEntity")
                || isSubclassNamed(entity.getClass(), "net.minecraft.entity.LivingEntity");
    }

    private static boolean isSubclassNamed(Class<?> clazz, String fqcn) {
        Class<?> current = clazz;
        while (current != null) {
            if (fqcn.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static Object getBrain(Object livingEntity) {
        try {
            Method getBrain = livingEntity.getClass().getMethod("getBrain");
            return getBrain.invoke(livingEntity);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean hasBrainType(Object brain) {
        String className = brain.getClass().getName();
        return "net.minecraft.world.entity.ai.Brain".equals(className)
                || "net.minecraft.entity.ai.brain.Brain".equals(className)
                || isSubclassNamed(brain.getClass(), "net.minecraft.world.entity.ai.Brain")
                || isSubclassNamed(brain.getClass(), "net.minecraft.entity.ai.brain.Brain");
    }

    private static Method findSetMemoryMethod(Class<?> brainClass) {
        for (Method method : brainClass.getMethods()) {
            if ("setMemory".equals(method.getName()) && method.getParameterCount() == 2) {
                return method;
            }
        }
        return null;
    }

    private static Field findMemoriesField(Class<?> brainClass) {
        for (Field field : brainClass.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }
}
