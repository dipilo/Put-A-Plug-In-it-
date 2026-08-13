/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - clearing EMF's held animation
 * iteration context off the renderer after each render. See NOTICE.md.
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

/**
 * Entity Model Features: clears {@code LivingEntityRenderer.emf$heldIteration} after each render.
 *
 * <p>Harvested from AllTheLeaks (MIT)
 * {@code leaks/client/mods/entity_model_features/UntrackedIssue00{1,2}}, which gate on EMF
 * {@code [2.2.6,3.0.6)}. Rather than parse EMF's version, this keys off the field itself: EMF 3.0.6
 * moved the iteration context to a MixinExtras {@code @Share} local, so on a fixed EMF the field is
 * simply absent and the fix stays off.
 */
final class EmfRenderContextFix {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MODID = "entity_model_features";
    private static final String LIVING_RENDERER_CLASS = "net.minecraft.client.renderer.entity.LivingEntityRenderer";
    private static final String HELD_ITERATION_FIELD = "emf$heldIteration";

    private static volatile boolean triedInit;
    private static volatile Field heldIteration;

    private EmfRenderContextFix() {
    }

    static void onLivingEntityRenderPost(Object renderer) {
        if (!Config.isEmfClearRenderContextOnRenderEndEnabled()) {
            return;
        }
        initIfNeeded();
        Field field = heldIteration;
        if (field == null) {
            return;
        }
        try {
            field.set(renderer, null);
        } catch (IllegalAccessException | RuntimeException e) {
            heldIteration = null;
            LOGGER.warn("[papi] Failed to clear EMF render context; disabling fix", e);
        }
    }

    private static void initIfNeeded() {
        if (triedInit) {
            return;
        }
        synchronized (EmfRenderContextFix.class) {
            if (triedInit) {
                return;
            }
            triedInit = true;

            if (!ModFixReflection.isModLoaded(MODID)) {
                return;
            }
            heldIteration = ModFixReflection.declaredField(
                    ModFixReflection.findClass(LIVING_RENDERER_CLASS), HELD_ITERATION_FIELD);
        }
    }
}
