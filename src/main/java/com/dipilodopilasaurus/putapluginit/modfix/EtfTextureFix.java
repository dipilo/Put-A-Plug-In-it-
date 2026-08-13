/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - clearing ETF's player texture map
 * on respawn, re-pointing the retained player on newer ETF, and clearing the
 * renderer's held entity/texture after each render. See NOTICE.md.
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
import java.util.Map;

/**
 * Entity Texture Features: drops the player-texture state that survives a respawn, and the entity
 * state each renderer holds after a render.
 *
 * <p>Harvested from AllTheLeaks (MIT)
 * {@code leaks/client/mods/entity_texture_features/UntrackedIssue00{1,2}}.
 *
 * <p>The respawn half is version-split: before ETF 7 clear {@code ETFManager.PLAYER_TEXTURE_MAP},
 * from 7 re-point the cached texture's {@code player} instead, which keeps the skin cache warm.
 * Selected on the presence of 7.x's {@code ETFPlayerTexture.player}, not by parsing a version.
 * Upstream's re-point also reads the old player, but only for a UUID a respawn preserves - so the
 * new player alone suffices and no entity is retained.
 */
final class EtfTextureFix {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MODID = "entity_texture_features";
    private static final String MANAGER_CLASS = "traben.entity_texture_features.features.ETFManager";
    private static final String TEXTURE_MAP_FIELD = "PLAYER_TEXTURE_MAP";
    private static final String PLAYER_TEXTURE_CLASS = "traben.entity_texture_features.features.player.ETFPlayerTexture";
    private static final String RETAINED_PLAYER_FIELD = "player";
    private static final String LIVING_RENDERER_CLASS = "net.minecraft.client.renderer.entity.LivingEntityRenderer";
    private static final String PLAYER_RENDERER_CLASS = "net.minecraft.client.renderer.entity.player.PlayerRenderer";
    private static final String HELD_ENTITY_FIELD = "etf$heldEntity";
    private static final String HELD_PLAYER_TEXTURE_FIELD = "etf$ETFPlayerTexture";

    private static volatile boolean triedInit;
    private static volatile boolean available;
    private static Class<?> managerClass;
    private static Field textureMapField;
    private static Field retainedPlayerField;

    private static volatile boolean triedRendererInit;
    private static volatile Field heldEntityField;
    private static volatile Field heldPlayerTextureField;
    private static Class<?> playerRendererClass;

    private EtfTextureFix() {
    }

    static void onClientRespawn(Object player) {
        if (!Config.isEtfClearPlayerTextureMapOnRespawnEnabled()) {
            return;
        }
        initIfNeeded();
        if (!available) {
            return;
        }
        try {
            Object manager = managerClass.getMethod("getInstance").invoke(null);
            if (manager == null || !(textureMapField.get(manager) instanceof Map<?, ?> textureMap)) {
                return;
            }
            if (retainedPlayerField == null) {
                // ETFLruCache extends LinkedHashMap, so this is a plain Map.clear().
                textureMap.clear();
            } else {
                retargetRetainedPlayer(textureMap, player);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            LOGGER.warn("[papi] Failed to release ETF player texture state; disabling fix", e);
        }
    }

    static void onLivingEntityRenderPost(Object renderer) {
        if (!Config.isEtfClearRendererStateOnRenderEndEnabled()) {
            return;
        }
        initRendererFieldsIfNeeded();
        try {
            Field heldEntity = heldEntityField;
            if (heldEntity != null) {
                heldEntity.set(renderer, null);
            }
            Field heldTexture = heldPlayerTextureField;
            if (heldTexture != null && playerRendererClass.isInstance(renderer)) {
                heldTexture.set(renderer, null);
            }
        } catch (IllegalAccessException | RuntimeException e) {
            heldEntityField = null;
            heldPlayerTextureField = null;
            LOGGER.warn("[papi] Failed to clear ETF renderer state; disabling fix", e);
        }
    }

    private static void retargetRetainedPlayer(Map<?, ?> textureMap, Object player) throws ReflectiveOperationException {
        if (player == null) {
            return;
        }
        Object uuid = player.getClass().getMethod("getUUID").invoke(player);
        Object texture = textureMap.get(uuid);
        if (texture != null && retainedPlayerField.getType().isInstance(player)) {
            retainedPlayerField.set(texture, player);
        }
    }

    private static void initIfNeeded() {
        if (triedInit) {
            return;
        }
        synchronized (EtfTextureFix.class) {
            if (triedInit) {
                return;
            }
            triedInit = true;

            if (!ModFixReflection.isModLoaded(MODID)) {
                return;
            }
            managerClass = ModFixReflection.findClass(MANAGER_CLASS);
            textureMapField = ModFixReflection.declaredField(managerClass, TEXTURE_MAP_FIELD);
            retainedPlayerField = ModFixReflection.declaredField(
                    ModFixReflection.findClass(PLAYER_TEXTURE_CLASS), RETAINED_PLAYER_FIELD);
            available = managerClass != null && textureMapField != null;
        }
    }

    private static void initRendererFieldsIfNeeded() {
        if (triedRendererInit) {
            return;
        }
        synchronized (EtfTextureFix.class) {
            if (triedRendererInit) {
                return;
            }
            triedRendererInit = true;

            if (!ModFixReflection.isModLoaded(MODID)) {
                return;
            }
            heldEntityField = ModFixReflection.declaredField(
                    ModFixReflection.findClass(LIVING_RENDERER_CLASS), HELD_ENTITY_FIELD);
            playerRendererClass = ModFixReflection.findClass(PLAYER_RENDERER_CLASS);
            heldPlayerTextureField = ModFixReflection.declaredField(playerRendererClass, HELD_PLAYER_TEXTURE_FIELD);
        }
    }
}
