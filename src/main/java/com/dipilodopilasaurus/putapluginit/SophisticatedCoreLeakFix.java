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
import java.util.Map;
import java.util.Optional;

public final class SophisticatedCoreLeakFix {
    private static final String GLOBAL_FIXED_VERSION = "1.4.6.1504";
    private static final String SOPHISTICATED_CORE_MODID = "sophisticatedcore";
    private static final String ITEMSTACKKEY_CLASS = "net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey";
    private static final String CACHE_FIELD = "CACHE";

    private static volatile boolean initTried = false;
    private static volatile boolean available = false;
    private static Field cacheField;
    private static int clientTickCounter = 0;

    private SophisticatedCoreLeakFix() {
    }

    @SuppressWarnings("java:S3011")
    private static void allowReflectiveAccess(Field field) {
        field.setAccessible(true);
    }

    public static void onClientTickEnd() {
        if (!Config.isFixSophisticatedCoreItemStackKeyCacheEnabled()) {
            return;
        }
        clientTickCounter++;
        int everyTicks = Math.max(1, Config.getScItemStackKeyCacheClientClearEveryTicks());
        if (clientTickCounter % everyTicks != 0) {
            return;
        }
        clearIfLargeEnough(Config.getScItemStackKeyCacheClientMinSizeBeforeClear());
    }

    public static void onServerTickEnd() {
        if (Config.isFixSophisticatedCoreItemStackKeyCacheEnabled()
                && Config.isScItemStackKeyCacheClearOnServerTickEnd()) {
            clearIfLargeEnough(Config.getScItemStackKeyCacheClientMinSizeBeforeClear());
        }
    }

    /**
     * Read-only diagnostic accessor for {@code LeakReporter}: reports the current size of
     * {@code ItemStackKey.CACHE}, or {@code "n/a"} when SophisticatedCore is absent or runs a
     * version that already ships the fix (in which case this mod does not touch the cache).
     */
    public static String tryGetItemStackKeyCacheSize() {
        initIfNeeded();
        if (!available || cacheField == null) {
            return "n/a";
        }
        try {
            Object cacheObj = cacheField.get(null);
            if (cacheObj instanceof Map<?, ?>) {
                return Integer.toString(((Map<?, ?>) cacheObj).size());
            }
            return "unexpected(" + cacheObj.getClass().getName() + ")";
        } catch (Exception t) {
            available = false;
            return "error(" + t.getClass().getSimpleName() + ")";
        }
    }

    private static void clearIfLargeEnough(int minSize) {
        initIfNeeded();
        if (!available || cacheField == null) {
            return;
        }

        try {
            Object cacheObj = cacheField.get(null);
            if (cacheObj instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>) cacheObj;
                if (map.size() >= minSize) {
                    map.clear();
                }
            }
        } catch (Exception ignored) {
            available = false;
        }
    }

    private static void initIfNeeded() {
        if (initTried) {
            return;
        }
        synchronized (SophisticatedCoreLeakFix.class) {
            if (initTried) {
                return;
            }
            initTried = true;

            String scVersion = detectSophisticatedCoreVersion();
            if (scVersion == null || !shouldApplyItemStackKeyFix(scVersion)) {
                available = false;
                return;
            }

            try {
                Class<?> keyClass = Class.forName(ITEMSTACKKEY_CLASS, false, SophisticatedCoreLeakFix.class.getClassLoader());
                Field field = keyClass.getDeclaredField(CACHE_FIELD);
                allowReflectiveAccess(field);
                cacheField = field;
                available = true;
            } catch (Exception ignored) {
                available = false;
            }
        }
    }

    private static String detectSophisticatedCoreVersion() {
        String fromForge = detectForgeLikeVersion("net.minecraftforge.fml.ModList");
        if (fromForge != null) {
            return fromForge;
        }

        String fromNeoForge = detectForgeLikeVersion("net.neoforged.fml.ModList");
        if (fromNeoForge != null) {
            return fromNeoForge;
        }

        return detectFabricVersion();
    }

    private static String detectForgeLikeVersion(String modListClassName) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, SophisticatedCoreLeakFix.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            boolean loaded = (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, SOPHISTICATED_CORE_MODID);
            if (!loaded) {
                return null;
            }

            Object maybeContainer = modListClass.getMethod("getModContainerById", String.class)
                    .invoke(modList, SOPHISTICATED_CORE_MODID);
            if (!(maybeContainer instanceof Optional<?>)) {
                return "";
            }

            Optional<?> optional = (Optional<?>) maybeContainer;
            if (!optional.isPresent()) {
                return "";
            }

            Object container = optional.get();
            Object modInfo = container.getClass().getMethod("getModInfo").invoke(container);
            Object version = modInfo.getClass().getMethod("getVersion").invoke(modInfo);
            return version.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String detectFabricVersion() {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, SophisticatedCoreLeakFix.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);

            boolean loaded = (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, SOPHISTICATED_CORE_MODID);
            if (!loaded) {
                return null;
            }

            Object maybeContainer = loaderClass.getMethod("getModContainer", String.class).invoke(loader, SOPHISTICATED_CORE_MODID);
            if (!(maybeContainer instanceof Optional<?>)) {
                return "";
            }

            Optional<?> optional = (Optional<?>) maybeContainer;
            if (!optional.isPresent()) {
                return "";
            }

            Object container = optional.get();
            Method getMetadata = container.getClass().getMethod("getMetadata");
            Object metadata = getMetadata.invoke(container);
            Object version = metadata.getClass().getMethod("getVersion").invoke(metadata);
            return version.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean shouldApplyItemStackKeyFix(String version) {
        ParsedVersion parsed = parseVersion(version);
        if (parsed == null) {
            return true;
        }

        if (compareDotVersion(parsed.scVersion(), GLOBAL_FIXED_VERSION) >= 0) {
            return false;
        }

        String mcThreshold = getMcSpecificFixedVersion(parsed.mcVersion());
        return mcThreshold == null || compareDotVersion(parsed.scVersion(), mcThreshold) < 0;
    }

    private static ParsedVersion parseVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }

        String[] parts = version.trim().toLowerCase().split("-", 2);
        if (parts.length != 2) {
            return null;
        }
        return new ParsedVersion(parts[0], parts[1]);
    }

    private static String getMcSpecificFixedVersion(String mcVersion) {
        if ("1.20.1".equals(mcVersion)) {
            return "1.3.5.1505";
        }
        if ("1.21.1".equals(mcVersion)) {
            return "1.4.5.1499";
        }
        if ("1.21.4".equals(mcVersion)) {
            return "1.4.5.1500";
        }
        if ("1.21.5".equals(mcVersion)) {
            return "1.4.5.1501";
        }
        if ("1.21.8".equals(mcVersion)) {
            return "1.4.5.1502";
        }
        if ("1.21.10".equals(mcVersion)) {
            return "1.4.6.1503";
        }
        if ("1.21.11".equals(mcVersion)) {
            return GLOBAL_FIXED_VERSION;
        }
        return null;
    }

    private static int compareDotVersion(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int len = Math.max(l.length, r.length);
        for (int i = 0; i < len; i++) {
            int li = i < l.length ? parsePart(l[i]) : 0;
            int ri = i < r.length ? parsePart(r[i]) : 0;
            if (li != ri) {
                return Integer.compare(li, ri);
            }
        }
        return 0;
    }

    private static int parsePart(String value) {
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

    private static final class ParsedVersion {
        private final String mcVersion;
        private final String scVersion;

        private ParsedVersion(String mcVersion, String scVersion) {
            this.mcVersion = mcVersion;
            this.scVersion = scVersion;
        }

        private String mcVersion() {
            return mcVersion;
        }

        private String scVersion() {
            return scVersion;
        }
    }
}