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

import java.lang.reflect.Field;

/**
 * Shared reflection + loader-detection helpers for the per-mod leak fixes under {@code modfix}.
 *
 * <p>The fixes harvested from AllTheLeaks (MIT) clear caches inside third-party mods that are not on
 * PAPI's compile classpath, so every access is by class-name through reflection and every fix
 * degrades to a no-op when the target mod (or the client) is absent.
 */
final class ModFixReflection {
    private ModFixReflection() {
    }

    /** The client {@code Minecraft} singleton, or {@code null} on a dedicated server. */
    static Object minecraft() {
        try {
            Class<?> mc = Class.forName("net.minecraft.client.Minecraft", false, ModFixReflection.class.getClassLoader());
            return mc.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null; // dedicated server, or the singleton is not up yet
        }
    }

    /** First declared field of {@code owner} whose declared type name equals {@code typeName}, or {@code null}. */
    @SuppressWarnings("java:S3011") // Reading private Minecraft fields is how this stays loader-agnostic.
    static Object firstFieldOfType(Object owner, String typeName) {
        for (Field f : owner.getClass().getDeclaredFields()) {
            if (!f.getType().getName().equals(typeName)) {
                continue;
            }
            try {
                f.setAccessible(true);
                return f.get(owner);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    /** Loads {@code name}, or {@code null} when the owning mod is absent. */
    static Class<?> findClass(String name) {
        try {
            return Class.forName(name, false, ModFixReflection.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    /** Accessible declared field of {@code owner}, or {@code null} when the mod version renamed it. */
    @SuppressWarnings("java:S3011") // Clearing third-party caches is what these fixes exist to do.
    static Field declaredField(Class<?> owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    /** Calls a static no-arg method if the class and method both exist. Returns false when it did not run. */
    static boolean invokeStaticNoArg(String className, String methodName) {
        Class<?> owner = findClass(className);
        if (owner == null) {
            return false;
        }
        try {
            owner.getMethod(methodName).invoke(null);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    static boolean isModLoaded(String modId) {
        return isForgeLikeLoaded("net.minecraftforge.fml.ModList", modId)
                || isForgeLikeLoaded("net.neoforged.fml.ModList", modId)
                || isFabricLoaded(modId);
    }

    private static boolean isForgeLikeLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, ModFixReflection.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isFabricLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, ModFixReflection.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
