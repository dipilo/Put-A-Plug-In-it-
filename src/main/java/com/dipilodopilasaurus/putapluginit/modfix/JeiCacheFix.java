/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - the JEI caches to clear on world
 * unload and respawn. See NOTICE.md.
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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JEI: on client level unload and on respawn, clears the {@code RecipeTransferManager}'s
 * unsupported-container set, drops {@code GrindstoneRecipeMaker}'s cached menu, and discards the
 * static ingredient codecs.
 *
 * <p>Harvested from AllTheLeaks (MIT) {@code leaks/client/mods/jei/{UntrackedIssue001,
 * UntrackedIssue002, UntrackedIssue004}}. None is evicted: the container set accumulates menus
 * holding a player and level, the static grindstone menu pins one for the session, and each codec
 * closes over the {@code IIngredientManager} of the runtime that built it.
 *
 * <p>Level unload stands in for upstream's {@code LoggingOut} and casts the wider net - quitting to
 * the menu strands the menu whether or not the connection is torn down.
 *
 * <p>The transfer manager is resolved per call off the live JEI runtime, which is replaced across
 * world loads; caching it would go stale and retain the old one.
 */
final class JeiCacheFix {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MODID = "jei";
    private static final String INTERNAL_CLASS = "mezz.jei.common.Internal";
    private static final String GRINDSTONE_CLASS = "mezz.jei.library.plugins.vanilla.grindstone.GrindstoneRecipeMaker";
    private static final String UNSUPPORTED_CONTAINERS_FIELD = "unsupportedContainers";
    private static final String GRINDSTONE_MENU_FIELD = "GRINDSTONE_MENU";
    private static final String CODECS_CLASS = "mezz.jei.common.codecs.TypedIngredientCodecs";
    private static final String[] CODEC_FIELDS = {"ingredientCodec", "ingredientTypeCodec"};
    private static final String CODEC_MAP_FIELD = "codecMapCache";
    private static final String OPTIONAL_RUNTIME_GETTER = "getOptionalJeiRuntime";
    private static final String RUNTIME_GETTER = "getJeiRuntime";

    private static volatile boolean triedInit;
    private static volatile boolean available;
    private static Method runtimeAccessor;
    private static Field grindstoneMenuField;
    private static Field[] codecFields = new Field[0];
    private static Field codecMapField;

    private JeiCacheFix() {
    }

    static void onClientWorldChange() {
        if (!enabled()) {
            return;
        }
        clearUnsupportedContainers();
        clearGrindstoneMenu();
        clearIngredientCodecs();
    }

    private static boolean enabled() {
        if (!Config.isJeiClearCachesOnWorldChangeEnabled()) {
            return false;
        }
        initIfNeeded();
        return available;
    }

    private static void clearUnsupportedContainers() {
        if (runtimeAccessor == null) {
            return;
        }
        try {
            Object result = runtimeAccessor.invoke(null);
            // getOptionalJeiRuntime hands back an empty Optional off-world; the plain getter throws
            // there instead, which is why it is only ever the fallback.
            Object runtime = result instanceof Optional<?> optional ? optional.orElse(null) : result;
            if (runtime == null) {
                return; // JEI runtime is only up while a world is loaded
            }
            Object manager = runtime.getClass().getMethod("getRecipeTransferManager").invoke(runtime);
            if (manager == null) {
                return;
            }
            Field field = ModFixReflection.declaredField(manager.getClass(), UNSUPPORTED_CONTAINERS_FIELD);
            if (field != null && field.get(manager) instanceof Collection<?> containers) {
                containers.clear();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("[papi] Failed to clear JEI unsupported-container set", e);
        }
    }

    @SuppressWarnings("java:S3011") // Reaching into JEI internals by reflection is the point of this fix.
    private static void clearGrindstoneMenu() {
        if (grindstoneMenuField == null) {
            return;
        }
        try {
            grindstoneMenuField.set(null, null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            LOGGER.warn("[papi] Failed to clear JEI grindstone menu; disabling fix", e);
        }
    }

    /** Both codecs and the per-type map are lazily rebuilt on the next read, so dropping them is safe. */
    @SuppressWarnings("java:S3011") // Reaching into JEI internals by reflection is the point of this fix.
    private static void clearIngredientCodecs() {
        try {
            for (Field field : codecFields) {
                field.set(null, null);
            }
            if (codecMapField != null && codecMapField.get(null) instanceof Map<?, ?> cache) {
                cache.clear();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            codecFields = new Field[0];
            codecMapField = null;
            LOGGER.warn("[papi] Failed to clear JEI ingredient codecs; disabling that half", e);
        }
    }

    private static void initIfNeeded() {
        if (triedInit) {
            return;
        }
        synchronized (JeiCacheFix.class) {
            if (triedInit) {
                return;
            }
            triedInit = true;

            if (!ModFixReflection.isModLoaded(MODID)) {
                return;
            }
            runtimeAccessor = findRuntimeAccessor(ModFixReflection.findClass(INTERNAL_CLASS));
            grindstoneMenuField = ModFixReflection.declaredField(
                    ModFixReflection.findClass(GRINDSTONE_CLASS), GRINDSTONE_MENU_FIELD);
            Class<?> codecs = ModFixReflection.findClass(CODECS_CLASS);
            codecFields = Arrays.stream(CODEC_FIELDS)
                    .map(name -> ModFixReflection.declaredField(codecs, name))
                    .filter(Objects::nonNull)
                    .toArray(Field[]::new);
            codecMapField = ModFixReflection.declaredField(codecs, CODEC_MAP_FIELD);
            available = runtimeAccessor != null || grindstoneMenuField != null
                    || codecFields.length > 0 || codecMapField != null;
        }
    }

    /** JEI 19.19 added the Optional getter; before it, the plain one is all there is. */
    private static Method findRuntimeAccessor(Class<?> internal) {
        if (internal == null) {
            return null;
        }
        for (String name : new String[]{OPTIONAL_RUNTIME_GETTER, RUNTIME_GETTER}) {
            try {
                return internal.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                // try the next name; a JEI with neither leaves this fix disabled
            }
        }
        return null;
    }
}
