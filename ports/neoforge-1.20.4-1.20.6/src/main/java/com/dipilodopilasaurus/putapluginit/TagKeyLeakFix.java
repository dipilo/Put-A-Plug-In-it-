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

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public final class TagKeyLeakFix {

    private static boolean attempted;

    private TagKeyLeakFix() {
    }

    public static void applyIfNeeded() {
        if (attempted) {
            return;
        }
        attempted = true;

        if (!isBelow119()) {
            return;
        }

        try {
            replaceTagKeyInterner();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Best-effort: if internals differ for a patch, skip silently.
        }
    }

    @SuppressWarnings("java:S3011")
    private static void replaceTagKeyInterner() throws ReflectiveOperationException {
        Class<?> tagKeyClass = Class.forName("net.minecraft.tags.TagKey", false, TagKeyLeakFix.class.getClassLoader());
        Field internerField = findStaticInternerField(tagKeyClass);
        if (internerField == null) {
            return;
        }
        internerField.setAccessible(true);
        removeFinalModifier(internerField);
        internerField.set(null, Interners.newWeakInterner());
    }

    private static boolean isBelow119() {
        try {
            Object worldVersion = resolveWorldVersion();
            String versionName = extractVersionName(worldVersion);
            return compareSimpleVersion(versionName, "1.19.0") < 0;
        } catch (RuntimeException ignored) {
            return false;
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

    private static Object resolveWorldVersion() {
        try {
            Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants", false, TagKeyLeakFix.class.getClassLoader());
            List<String> methodNames = Arrays.asList("getCurrentVersion", "getGameVersion", "createGameVersion");
            for (String methodName : methodNames) {
                Method method = findNoArgMethod(sharedConstantsClass, methodName);
                if (method != null) {
                    return method.invoke(null);
                }
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private static Method findNoArgMethod(Class<?> owner, String methodName) {
        try {
            return owner.getMethod(methodName);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static String extractVersionName(Object worldVersion) {
        if (worldVersion == null) {
            return "";
        }
        Method getName = findNoArgMethod(worldVersion.getClass(), "getName");
        if (getName != null) {
            try {
                Object value = getName.invoke(worldVersion);
                return value == null ? "" : value.toString();
            } catch (ReflectiveOperationException ignored) {
                // Continue fallback chain.
            }
        }
        Method getId = findNoArgMethod(worldVersion.getClass(), "getId");
        if (getId != null) {
            try {
                Object value = getId.invoke(worldVersion);
                return value == null ? "" : value.toString();
            } catch (ReflectiveOperationException ignored) {
                // Continue fallback chain.
            }
        }
        return worldVersion.toString();
    }

    private static Field findStaticInternerField(Class<?> tagKeyClass) {
        for (Field field : tagKeyClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Interner.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }

    @SuppressWarnings("java:S3011")
    private static void removeFinalModifier(Field field) {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (ReflectiveOperationException ignored) {
            // Java version dependent; if unavailable, leave as-is and try direct set.
        }
    }
}