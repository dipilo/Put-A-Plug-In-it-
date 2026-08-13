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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lwjgl.opengl.GL11C;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Method;
import java.util.Optional;

public final class ImmediatelyFastCompat {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String IF_MODID = "immediatelyfast";
    private static final String MLF_MODID = "memoryleakfix";

    private static boolean patched = false;

    private ImmediatelyFastCompat() {
    }

    public static void onClientTickEnd() {
        if (patched) {
            return;
        }
        patched = true;

        if (!isModLoaded(IF_MODID)) {
            return;
        }

        Path cfg = Paths.get("config", "immediatelyfast.json");
        if (!Files.exists(cfg)) {
            return;
        }

        try {
            JsonObject root = readJson(cfg);
            if (root == null) {
                return;
            }

            boolean changed = false;
            if (isIntelUhd()) {
                changed |= setBoolean(root, "experimental_screen_batching", false);
            }

            if (isModLoaded(MLF_MODID)) {
                changed |= setBoolean(root, "experimental_screen_batching", false);
            }

            if (changed) {
                writeJson(cfg, root);
            }
        } catch (IOException | RuntimeException ignored) {
            // Best-effort compatibility patch: ignore malformed or transiently unavailable config state.
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = new JsonParser().parse(reader);
            if (!element.isJsonObject()) {
                return null;
            }
            return element.getAsJsonObject();
        }
    }

    private static void writeJson(Path path, JsonObject root) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static boolean setBoolean(JsonObject root, String key, boolean value) {
        if (root.has(key) && root.get(key).isJsonPrimitive() && root.get(key).getAsBoolean() == value) {
            return false;
        }
        root.addProperty(key, value);
        return true;
    }

    private static boolean isIntelUhd() {
        try {
            String vendor = GL11C.glGetString(GL11C.GL_VENDOR);
            String renderer = GL11C.glGetString(GL11C.GL_RENDERER);
            String v = vendor == null ? "" : vendor.toLowerCase();
            String r = renderer == null ? "" : renderer.toLowerCase();
            return (v.contains("intel") || r.contains("intel"))
                    && (r.contains("uhd") || r.contains("iris xe") || r.contains("iris(r) xe"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isModLoaded(String modId) {
        return isForgeLikeLoaded("net.minecraftforge.fml.ModList", modId)
                || isForgeLikeLoaded("net.neoforged.fml.ModList", modId)
                || isFabricLoaded(modId);
    }

    private static boolean isForgeLikeLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, ImmediatelyFastCompat.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isFabricLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, ImmediatelyFastCompat.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);

            boolean loaded = (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
            if (!loaded) {
                return false;
            }

            Method getModContainer = loaderClass.getMethod("getModContainer", String.class);
            Object maybeContainer = getModContainer.invoke(loader, modId);
            if (!(maybeContainer instanceof Optional<?>)) {
                return false;
            }
            Optional<?> optional = (Optional<?>) maybeContainer;
            return optional.isPresent();
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}