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
package com.dipilodopilasaurus.putapluginit.modfix;

import com.dipilodopilasaurus.putapluginit.Config;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Jade: clears the {@code ObjectDataCenter} accessor and the {@code JadeClient.hideModName} cache
 * when the client level changes, and clears {@code ObjectDataCenter} again on respawn.
 *
 * <p>Harvested from AllTheLeaks (MIT) {@code leaks/client/mods/jade/UntrackedIssue001}: these hold
 * references to the last-looked-at block/entity and its level, which otherwise survive a world
 * transition. Loader-agnostic via reflection; no-ops when Jade is absent.
 */
final class JadeCacheFix {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MODID = "jade";
    private static final String OBJECT_DATA_CENTER_CLASS = "snownee.jade.impl.ObjectDataCenter";
    private static final String JADE_CLIENT_CLASS = "snownee.jade.JadeClient";

    private static volatile boolean triedInit;
    private static volatile boolean available;
    private static Method setAccessorMethod;
    private static Field hideModNameField;
    private static Method invalidateAllMethod;

    private JadeCacheFix() {
    }

    static void onClientLevelUnload() {
        if (!Config.isJadeClearCachesOnWorldChangeEnabled()) {
            return;
        }
        initIfNeeded();
        if (!available) {
            return;
        }
        clearObjectDataCenter();
        invalidateHideModNameCache();
    }

    static void onClientRespawn() {
        if (!Config.isJadeClearCachesOnWorldChangeEnabled()) {
            return;
        }
        initIfNeeded();
        if (!available) {
            return;
        }
        clearObjectDataCenter();
    }

    private static void clearObjectDataCenter() {
        try {
            setAccessorMethod.invoke(null, new Object[]{null});
        } catch (ReflectiveOperationException e) {
            available = false;
            LOGGER.warn("[papi] Failed to clear Jade ObjectDataCenter; disabling fix", e);
        }
    }

    private static void invalidateHideModNameCache() {
        try {
            Object cache = hideModNameField.get(null);
            if (cache != null) {
                invalidateAllMethod.invoke(cache);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[papi] Failed to invalidate Jade hideModName cache", e);
        }
    }

    private static void initIfNeeded() {
        if (triedInit) {
            return;
        }
        synchronized (JadeCacheFix.class) {
            if (triedInit) {
                return;
            }
            triedInit = true;

            if (!ModFixReflection.isModLoaded(MODID)) {
                return;
            }
            try {
                ClassLoader loader = JadeCacheFix.class.getClassLoader();
                Class<?> odc = Class.forName(OBJECT_DATA_CENTER_CLASS, false, loader);
                Method set = findSingleArgMethod(odc, "set");
                if (set == null) {
                    return;
                }
                set.setAccessible(true);

                Class<?> jadeClient = Class.forName(JADE_CLIENT_CLASS, false, loader);
                Field hideModName = jadeClient.getDeclaredField("hideModName");
                hideModName.setAccessible(true);

                // Guava's LocalCache$LocalManualCache is package-private in a named module, so
                // invalidateAll resolves off the Cache interface; off the instance it throws IAE.
                Method invalidateAll = hideModName.getType().getMethod("invalidateAll");

                setAccessorMethod = set;
                hideModNameField = hideModName;
                invalidateAllMethod = invalidateAll;
                available = true;
            } catch (ReflectiveOperationException ignored) {
                available = false;
            }
        }
    }

    private static Method findSingleArgMethod(Class<?> owner, String name) {
        for (Method method : owner.getDeclaredMethods()) {
            if (name.equals(method.getName()) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }
}
