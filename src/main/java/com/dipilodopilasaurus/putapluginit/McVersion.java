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

import java.lang.reflect.Method;

/**
 * Resolves the running Minecraft version for the fixes that self-gate to a version window.
 *
 * <p>A node advertises a <em>range</em>, so the build-time version cannot answer this: the
 * 1.19.2-fabric jar also runs on 1.18.2, which is the only version {@link TagKeyLeakFix} targets.
 * It has to be asked at runtime.
 *
 * <p>Every source below is deliberately a <strong>loader</strong> class or a system property, never a
 * Minecraft member: names passed to {@code Class.forName}/{@code getMethod} are not remapped, so
 * {@code SharedConstants.getCurrentVersion()} ({@code class_155} on Fabric, {@code m_183709_} on
 * Forge) answers on mojmap-native NeoForge alone. It is kept last, as a backstop.
 */
public final class McVersion {
    private static boolean resolved;
    private static String version = "";

    private McVersion() {
    }

    /** The running Minecraft version, or {@code ""} when no source could answer. */
    public static String get() {
        if (resolved) {
            return version;
        }
        resolved = true;
        version = resolve();
        return version;
    }

    /** {@code true} when the running version is inside {@code [floorInclusive, ceilingExclusive)}. */
    public static boolean isBetween(String floorInclusive, String ceilingExclusive) {
        String current = get();
        if (current.isEmpty()) {
            return false;
        }
        return compare(current, floorInclusive) >= 0 && compare(current, ceilingExclusive) < 0;
    }

    /** {@code true} when the running version is exactly {@code target}. */
    public static boolean is(String target) {
        return target.equals(get());
    }

    private static String resolve() {
        String candidate = fromFabricLoader();
        if (!candidate.isEmpty()) {
            return candidate;
        }
        // Both expose getMCVersion(); NeoForge dropped NeoFormVersion after 20.4
        for (String holder : new String[]{
                "net.minecraftforge.versions.mcp.MCPVersion",
                "net.neoforged.neoforge.internal.versions.neoform.NeoFormVersion"}) {
            candidate = fromStaticStringGetter(holder, "getMCVersion");
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        for (String property : new String[]{"fml.mcVersion", "minecraft.version", "fabric.gameVersion"}) {
            candidate = trimToEmpty(System.getProperty(property));
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return fromSharedConstants();
    }

    private static String fromFabricLoader() {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, McVersion.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Object container = loaderClass.getMethod("getModContainer", String.class).invoke(loader, "minecraft");
            Object present = container.getClass().getMethod("orElse", Object.class).invoke(container, (Object) null);
            if (present == null) {
                return "";
            }
            Object metadata = present.getClass().getMethod("getMetadata").invoke(present);
            Object semver = metadata.getClass().getMethod("getVersion").invoke(metadata);
            return trimToEmpty((String) semver.getClass().getMethod("getFriendlyString").invoke(semver));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    private static String fromStaticStringGetter(String className, String methodName) {
        try {
            Class<?> owner = Class.forName(className, false, McVersion.class.getClassLoader());
            return trimToEmpty((String) owner.getMethod(methodName).invoke(null));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    /** Only ever answers on a mojmap-native runtime; see the class Javadoc. */
    private static String fromSharedConstants() {
        try {
            Class<?> sharedConstants = Class.forName("net.minecraft.SharedConstants", false, McVersion.class.getClassLoader());
            for (String accessor : new String[]{"getCurrentVersion", "getGameVersion", "createGameVersion"}) {
                Object worldVersion = invokeStatic(sharedConstants, accessor);
                if (worldVersion == null) {
                    continue;
                }
                String name = invokeStringGetter(worldVersion, "getName");
                return name.isEmpty() ? invokeStringGetter(worldVersion, "getId") : name;
            }
        } catch (ClassNotFoundException | RuntimeException ignored) {
            // Intermediary-named runtime, or a stripped SharedConstants; the callers fail closed
        }
        return "";
    }

    private static Object invokeStatic(Class<?> owner, String methodName) {
        try {
            Method method = owner.getMethod(methodName);
            return method.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String invokeStringGetter(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    /**
     * Compares dotted release versions numerically
     * Snapshots and pre-releases sort by their numeric prefix
     */
    private static int compare(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int l = index < leftParts.length ? numericPrefix(leftParts[index]) : 0;
            int r = index < rightParts.length ? numericPrefix(rightParts[index]) : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private static int numericPrefix(String value) {
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

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
