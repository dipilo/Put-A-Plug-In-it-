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
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public final class DrownedNavigationLeakFix {

    private static final String MLF_MODID = "memoryleakfix";
    private static final Map<Object, Boolean> CLEANED_ENTITIES = new WeakHashMap<>();
    private static boolean checkedEnabled;
    private static boolean enabled;

    private DrownedNavigationLeakFix() {
    }

    public static void onEntityRemoved(Object entity, Object level) {
        if (!isEnabled() || entity == null || level == null || !isDrowned(entity)) {
            return;
        }
        synchronized (CLEANED_ENTITIES) {
            if (CLEANED_ENTITIES.put(entity, Boolean.TRUE) != null) {
                return;
            }
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
            return compareSimpleVersion(versionName, "1.17.0") < 0;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String resolveMinecraftVersionName() {
        try {
            Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants", false, DrownedNavigationLeakFix.class.getClassLoader());
            Object worldVersion = invokeFirstNoArgMethod(sharedConstantsClass, "getCurrentVersion", "getGameVersion", "createGameVersion");
            return extractVersionName(worldVersion);
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static Object invokeFirstNoArgMethod(Class<?> owner, String... methodNames) throws ReflectiveOperationException {
        for (String methodName : methodNames) {
            try {
                return owner.getMethod(methodName).invoke(null);
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
            Object value = worldVersion.getClass().getMethod("getName").invoke(worldVersion);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            try {
                Object value = worldVersion.getClass().getMethod("getId").invoke(worldVersion);
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

    private static boolean isDrowned(Object entity) {
        Class<?> current = entity.getClass();
        while (current != null && current != Object.class) {
            if (current.getName().contains("Drowned")) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
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
        return java.util.Collections.emptySet();
    }

    @SuppressWarnings({"unchecked", "java:S3011"})
    private static Set<Object> readNavigationSetField(Object level, Field field) {
        if (!Set.class.isAssignableFrom(field.getType())
                || !field.getName().toLowerCase().contains("navigation")) {
            return java.util.Collections.emptySet();
        }
        try {
            field.setAccessible(true);
            Object value = field.get(level);
            if (value instanceof Set<?>) {
                return (Set<Object>) value;
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
            return java.util.Collections.emptySet();
        }
        return java.util.Collections.emptySet();
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
            // Best effort
        }
    }

    private static boolean isModLoaded(String modId) {
        return isForgeLikeModLoaded("net.minecraftforge.fml.ModList", modId)
                || isForgeLikeModLoaded("net.neoforged.fml.ModList", modId)
                || isFabricModLoaded(modId);
    }

    private static boolean isForgeLikeModLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, DrownedNavigationLeakFix.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, DrownedNavigationLeakFix.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}