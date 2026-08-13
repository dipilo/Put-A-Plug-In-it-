/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 *
 * Adapted from MemoryLeakFix by Fx Morin (fxmorin / ca.fxco)
 * https://github.com/fxmorin/memoryLeakFix
 * Original work Copyright (C) Fx Morin. Original license: GNU LGPL-2.1-only.
 *
 * Changed by dipilo and PAPI contributors on 2026-03-07; modified through 2026.
 * Modifications Copyright (C) 2026 dipilo and PAPI contributors.
 *
 * Merged into Put A Plug In it! with the author's explicit written permission,
 * granted on the condition that MemoryLeakFix is credited. See NOTICE.md.
 *
 * This file remains licensed under the GNU Lesser General Public License,
 * version 2.1 ONLY. It is not relicensed to LGPL-3.0 and cannot be, because
 * MemoryLeakFix is LGPL-2.1-only. You may redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1 as
 * published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 2.1 along with this file; see licenses/LGPL-2.1.txt. If not, see
 * <https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>.
 */
package com.dipilodopilasaurus.putapluginit;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

/**
 * Clears a {@link LivingEntity}'s brain memories when it unloads, on the 1.19.0-1.19.3 window where
 * a memory could pin the entity it referred to. Vanilla fixed this in 1.19.4.
 *
 * <p>{@link LivingEntity}, {@link Brain} and {@link MemoryModuleType} are referenced as types, never
 * by name, so the loader remaps them - a string handed to {@code Class.forName} is not remapped, and
 * matching {@code "net.minecraft.world.entity.LivingEntity"} against Fabric's {@code class_1309}
 * silently cleared nothing. The one reflective step left, {@code Brain.memories}, matches on field
 * <em>type</em> and needs no name.
 */
public final class EntityMemoriesLeakFix {
    private static final String MLF_MODID = "memoryleakfix";

    private static boolean checkedEnabled = false;
    private static boolean enabled = false;

    private EntityMemoriesLeakFix() {
    }

    public static void onEntityRemoved(Object entity) {
        if (!isEnabled() || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }
        clearBrainMemories(livingEntity.getBrain());
    }

    private static boolean isEnabled() {
        if (checkedEnabled) {
            return enabled;
        }
        checkedEnabled = true;
        enabled = McVersion.isBetween("1.19.0", "1.19.4") && !isModLoaded(MLF_MODID);
        return enabled;
    }

    private static boolean isModLoaded(String modId) {
        if (isForgeLikeModLoaded("net.minecraftforge.fml.ModList", modId)) {
            return true;
        }
        if (isForgeLikeModLoaded("net.neoforged.fml.ModList", modId)) {
            return true;
        }
        return isFabricModLoaded(modId);
    }

    private static boolean isForgeLikeModLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, EntityMemoriesLeakFix.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, EntityMemoriesLeakFix.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked", "java:S3011"})
    private static void clearBrainMemories(Brain<?> brain) {
        Field memoriesField = findMemoriesField(brain.getClass());
        if (memoriesField == null) {
            return;
        }

        try {
            memoriesField.setAccessible(true);
            if (!(memoriesField.get(brain) instanceof Map<?, ?> memories)) {
                return;
            }
            // eraseMemory, not setMemory: the latter is overloaded on (type, U) and (type, Optional),
            // and the old reflective lookup could pick the former and store the Optional as the value.
            for (Object key : new ArrayList<>(memories.keySet())) {
                if (key instanceof MemoryModuleType<?> memoryType) {
                    brain.eraseMemory((MemoryModuleType) memoryType);
                }
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
            // Best-effort cleanup; if internals differ in a patch, skip silently.
        }
    }

    /** Matched by type: {@code memories} is the first {@code Map} field Brain declares on 1.19.x. */
    private static Field findMemoriesField(Class<?> brainClass) {
        for (Field field : brainClass.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }
}