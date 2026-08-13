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
package com.dipilodopilasaurus.putapluginit.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class PapiMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String PORTED_MLF_MIXIN_PACKAGE = "com.dipilodopilasaurus.putapluginit.mixin.memoryleakfix.";
    private static final String MLF_MOD_ID = "memoryleakfix";
    private static final String EMF_MOD_ID = "entity_model_features";
    private static final String ETF_MOD_ID = "entity_texture_features";
    private boolean checkedMemoryLeakFixPresence;
    private boolean memoryLeakFixPresent;
    private boolean checkedVersion;
    private String minecraftVersion = "";

    @Override
    public void onLoad(String mixinPackage) {
        resolveMinecraftVersion();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        resolveMemoryLeakFixPresence();
        resolveMinecraftVersion();
        if (memoryLeakFixPresent && mixinClassName.startsWith(PORTED_MLF_MIXIN_PACKAGE)) {
            return false;
        }
        if (!mixinClassName.startsWith(PORTED_MLF_MIXIN_PACKAGE)) {
            // The only injection PAPI makes in a per-entity, per-frame path; skip it when nothing needs it.
            if (mixinClassName.endsWith("LivingEntityRenderPostMixin")) {
                return isModLoaded(EMF_MOD_ID) || isModLoaded(ETF_MOD_ID);
            }
            return true;
        }

        if (mixinClassName.endsWith("TextureUtilFreeBufferMixin")) {
            // 1.19.4 is where vanilla wrapped the read in its own memFree-on-IOException handler.
            return below("1.19.4");
        }

        if (mixinClassName.endsWith("MinecraftScreenshotMixin")) {
            // grabHugeScreenshot is gone from 1.21.5; applying anyway only adds a @Unique field.
            return below("1.21.5");
        }

        return true;
    }

    private void resolveMinecraftVersion() {
        if (checkedVersion) {
            return;
        }
        checkedVersion = true;
        String version = System.getProperty("fml.mcVersion");
        if (version == null || version.isEmpty()) {
            version = System.getProperty("minecraft.version");
        }
        if (version == null || version.isEmpty()) {
            version = System.getProperty("minecraftVersion");
        }
        if (version == null || version.isEmpty()) {
            version = System.getProperty("fabric.gameVersion");
        }
        minecraftVersion = version == null ? "" : version;
    }

    private boolean below(String version) {
        return compareVersions(minecraftVersion, version) < 0;
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = sanitize(left).split("\\.");
        String[] rightParts = sanitize(right).split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int l = index < leftParts.length ? parseInt(leftParts[index]) : 0;
            int r = index < rightParts.length ? parseInt(rightParts[index]) : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "0";
        }
        String base = value.trim();
        int space = base.indexOf(' ');
        if (space >= 0) {
            base = base.substring(0, space);
        }
        return base.replaceAll("[^0-9.]", "");
    }

    private static int parseInt(String part) {
        if (part == null || part.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * Only a positive result is latched. Forge/NeoForge's {@code ModList} is not up when the mixin
     * config loads, so an early probe answers "absent" for a MemoryLeakFix that is in fact installed;
     * re-probing until it says yes keeps that from being cached for the session.
     */
    private void resolveMemoryLeakFixPresence() {
        if (checkedMemoryLeakFixPresence) {
            return;
        }
        memoryLeakFixPresent = isModLoaded(MLF_MOD_ID);
        checkedMemoryLeakFixPresence = memoryLeakFixPresent;
    }

    /**
     * Each probe swallows its own failure: on Fabric the Forge {@code ModList} lookup throws
     * {@code ClassNotFoundException}, and a shared catch would short-circuit the Fabric probe.
     */
    private static boolean isModLoaded(String modId) {
        return isForgeLikeModLoaded("net.minecraftforge.fml.ModList", modId)
                || isForgeLikeModLoaded("net.neoforged.fml.ModList", modId)
                || isFabricModLoaded(modId);
    }

    private static boolean isForgeLikeModLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, PapiMixinConfigPlugin.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false; // not this loader, or the mod list is not up yet
        }
    }

    private static boolean isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, PapiMixinConfigPlugin.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false; // not this loader, or the mod list is not up yet
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No target remapping required for this plugin.
    }

    @Override
    public List<String> getMixins() {
        return Collections.emptyList();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No pre-apply bytecode mutation required.
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No post-apply bytecode mutation required.
    }
}