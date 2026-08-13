/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - the set of Iceberg caches to clear
 * on client level unload. See NOTICE.md.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version. See <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit.modfix;

import com.dipilodopilasaurus.putapluginit.Config;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Iceberg: on client level unload, clears {@code EntityCollector}'s wrapped-level and entity caches
 * and drops {@code CustomItemRenderer}'s cached display entities.
 *
 * <p>Harvested from AllTheLeaks (MIT) {@code leaks/client/mods/iceberg/{Issue76,UntrackedIssue001}}.
 * Both hold entities (and through them their level) past the level's lifetime. AllTheLeaks reaches
 * the renderer fields through a Mixin accessor; reflection keeps this loader-agnostic and mixin-free.
 * Every field is optional, so older Iceberg versions simply clear fewer of them.
 */
final class IcebergCacheFix {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MODID = "iceberg";
    private static final String ENTITY_COLLECTOR_CLASS = "com.anthonyhilyard.iceberg.util.EntityCollector";
    private static final String ITEM_RENDERER_CLASS = "com.anthonyhilyard.iceberg.renderer.CustomItemRenderer";
    private static final String[] COLLECTOR_MAPS = {"wrappedLevelsMap", "entityCache"};
    private static final String[] RENDERER_ENTITIES = {"wolf", "horse", "armorStand", "entity"};

    private static volatile boolean triedInit;
    private static volatile boolean available;
    private static final List<Field> MAP_FIELDS = new ArrayList<>();
    private static final List<Field> ENTITY_FIELDS = new ArrayList<>();

    private IcebergCacheFix() {
    }

    static void onClientLevelUnload() {
        if (!Config.isIcebergClearCachesOnWorldUnloadEnabled()) {
            return;
        }
        initIfNeeded();
        if (!available) {
            return;
        }
        clearMaps();
        clearEntities();
    }

    private static void clearMaps() {
        for (Field field : MAP_FIELDS) {
            try {
                Object value = field.get(null);
                if (value instanceof Map<?, ?> map) {
                    map.clear();
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                available = false;
                LOGGER.warn("[papi] Failed to clear Iceberg {}; disabling fix", field.getName(), e);
                return;
            }
        }
    }

    private static void clearEntities() {
        for (Field field : ENTITY_FIELDS) {
            try {
                field.set(null, null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                available = false;
                LOGGER.warn("[papi] Failed to clear Iceberg CustomItemRenderer.{}; disabling fix", field.getName(), e);
                return;
            }
        }
    }

    private static void initIfNeeded() {
        if (triedInit) {
            return;
        }
        synchronized (IcebergCacheFix.class) {
            if (triedInit) {
                return;
            }
            triedInit = true;

            if (!ModFixReflection.isModLoaded(MODID)) {
                return;
            }
            collect(ModFixReflection.findClass(ENTITY_COLLECTOR_CLASS), COLLECTOR_MAPS, MAP_FIELDS);
            collect(ModFixReflection.findClass(ITEM_RENDERER_CLASS), RENDERER_ENTITIES, ENTITY_FIELDS);
            available = !MAP_FIELDS.isEmpty() || !ENTITY_FIELDS.isEmpty();
        }
    }

    private static void collect(Class<?> owner, String[] names, List<Field> into) {
        for (String name : names) {
            Field field = ModFixReflection.declaredField(owner, name);
            if (field != null) {
                into.add(field);
            }
        }
    }
}
