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
package com.dipilodopilasaurus.putapluginit.leaks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Read-only diagnostic snapshot of the client model system for {@link LeakReporter}.
 */
final class MinecraftClientWatch {
    private static final String MC_MINECRAFT = "net.minecraft.client.Minecraft";
    private static final String MC_MODEL_MANAGER = "net.minecraft.client.resources.model.ModelManager";
    private static final String MC_BLOCK_STATE = "net.minecraft.world.level.block.state.BlockState";
    private static final String MC_BAKED_MODEL = "net.minecraft.client.resources.model.BakedModel";
    private static final String MC_MULTIPART = "net.minecraft.client.resources.model.MultiPartBakedModel";
    private static final String FASTUTIL_OBJECT2INT_MAP_CLASS = "it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap";

    private MinecraftClientWatch() {
    }

    static ClientSnapshot trySnapshot() {
        try {
            Object minecraft = getMinecraftInstance();
            if (minecraft == null) {
                return ClientSnapshot.notClient();
            }

            Object modelManager = getFirstFieldByTypeName(minecraft, MC_MODEL_MANAGER);
            if (modelManager == null) {
                return new ClientSnapshot(true, "missing ModelManager", null, List.of());
            }

            Integer blockStateIdMapSize = findBlockStateToIntMapSize(modelManager);
            List<MapSummary> bakedMaps = findLikelyBakedModelMaps(modelManager);

            return new ClientSnapshot(true, null, blockStateIdMapSize, bakedMaps);
        } catch (Exception t) {
            return new ClientSnapshot(true, t.getClass().getSimpleName() + ": " + safeMsg(t.getMessage()), null, List.of());
        }
    }

    private static Object getMinecraftInstance() throws ReflectiveOperationException {
        try {
            Class<?> mcClazz = Class.forName(MC_MINECRAFT, false, MinecraftClientWatch.class.getClassLoader());
            return mcClazz.getMethod("getInstance").invoke(null);
        } catch (ClassNotFoundException e) {
            return null; // dedicated server
        }
    }

    private static Object getFirstFieldByTypeName(Object instance, String typeName) {
        for (Field f : instance.getClass().getDeclaredFields()) {
            if (!f.getType().getName().equals(typeName)) {
                continue;
            }
            try {
                allowReflectiveAccess(f);
                return f.get(instance);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("java:S3011")
    private static void allowReflectiveAccess(Field field) {
        field.trySetAccessible();
    }

    private static Integer findBlockStateToIntMapSize(Object modelManager) {
        // The MAT suspect shows ModelManager holds an Object2IntOpenHashMap with BlockState keys
        for (Field f : modelManager.getClass().getDeclaredFields()) {
            Object value = readFieldValue(modelManager, f);
            if (isBlockStateKeyedObject2IntMap(value)) {
                try {
                    return (int) value.getClass().getMethod("size").invoke(value);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static List<MapSummary> findLikelyBakedModelMaps(Object modelManager) {
        List<MapSummary> summaries = new ArrayList<>();

        for (Field f : modelManager.getClass().getDeclaredFields()) {
            Object value = readFieldValue(modelManager, f);
            if (value instanceof Map<?, ?> map) {
                MapSummary ms = summarizeMapIfBakedModelValues(map);
                if (ms != null) {
                    summaries.add(ms);
                }
            }
        }

        summaries.sort(Comparator.comparingInt(MapSummary::size).reversed());
        if (summaries.size() > 3) {
            return List.copyOf(summaries.subList(0, 3));
        }
        return List.copyOf(summaries);
    }

    private static MapSummary summarizeMapIfBakedModelValues(Map<?, ?> map) {
        int size = getMapSize(map);
        if (size < 10_000) {
            return null;
        }

        SampleSummary sample = sampleMapValues(map);
        if (!isLikelyBakedModelMap(sample)) {
            return null;
        }

        MultiPartSummary multiparts = countMultiParts(map);
        return new MapSummary(size, sample.exampleValueType(), multiparts.count(), multiparts.partial(), sample.sampleCount(), sample.multipartInSample());
    }

    private static Object readFieldValue(Object owner, Field field) {
        try {
            allowReflectiveAccess(field);
            return field.get(owner);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isBlockStateKeyedObject2IntMap(Object value) {
        if (value == null) {
            return false;
        }
        if (!isInstanceOf(value, FASTUTIL_OBJECT2INT_MAP_CLASS)) {
            return false;
        }
        try {
            Object keySet = value.getClass().getMethod("keySet").invoke(value);
            Object iterator = keySet.getClass().getMethod("iterator").invoke(keySet);
            boolean hasNext = (boolean) iterator.getClass().getMethod("hasNext").invoke(iterator);
            if (!hasNext) {
                return false;
            }
            Object firstKey = iterator.getClass().getMethod("next").invoke(iterator);
            return firstKey != null && MC_BLOCK_STATE.equals(firstKey.getClass().getName());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int getMapSize(Map<?, ?> map) {
        try {
            return map.size();
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static SampleSummary sampleMapValues(Map<?, ?> map) {
        int sampled = 0;
        int multiparts = 0;
        String exampleValueType = null;
        Object firstNonNull = null;
        for (Object value : map.values()) {
            if (value != null) {
                sampled++;
                if (firstNonNull == null) {
                    firstNonNull = value;
                    exampleValueType = value.getClass().getName();
                }
                if (MC_MULTIPART.equals(value.getClass().getName())) {
                    multiparts++;
                }
                if (sampled >= 256) {
                    return new SampleSummary(sampled, multiparts, exampleValueType, firstNonNull);
                }
            }
        }
        return new SampleSummary(sampled, multiparts, exampleValueType, firstNonNull);
    }

    private static boolean isInstanceOf(Object value, String className) {
        try {
            Class<?> clazz = Class.forName(className, false, MinecraftClientWatch.class.getClassLoader());
            return clazz.isInstance(value);
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isLikelyBakedModelMap(SampleSummary sample) {
        return sample.firstNonNullValue() != null && implementsInterfaceByName(sample.firstNonNullValue(), MC_BAKED_MODEL);
    }

    private static MultiPartSummary countMultiParts(Map<?, ?> map) {
        int multipartCount = 0;
        int iter = 0;
        try {
            for (Object value : map.values()) {
                if (value != null && MC_MULTIPART.equals(value.getClass().getName())) {
                    multipartCount++;
                }
                iter++;
                if (iter >= 400_000) {
                    return new MultiPartSummary(multipartCount, true);
                }
            }
            return new MultiPartSummary(multipartCount, false);
        } catch (Exception ignored) {
            return new MultiPartSummary(-1, false);
        }
    }

    private static boolean implementsInterfaceByName(Object instance, String interfaceName) {
        if (instance == null) {
            return false;
        }
        Class<?> c = instance.getClass();
        while (c != null) {
            for (Class<?> i : c.getInterfaces()) {
                if (i.getName().equals(interfaceName)) {
                    return true;
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static String safeMsg(String msg) {
        return msg == null ? "" : msg;
    }

    record ClientSnapshot(boolean isClient, String error, Integer blockStateToIntMapSize, List<MapSummary> bakedModelMaps) {
        static ClientSnapshot notClient() {
            return new ClientSnapshot(false, null, null, List.of());
        }
    }

    record MapSummary(int size, String exampleValueType, int multipartCount, boolean multipartPartial,
                      int sampleCount, int multipartInSample) {
    }

    record SampleSummary(int sampleCount, int multipartInSample, String exampleValueType, Object firstNonNullValue) {
    }

    record MultiPartSummary(int count, boolean partial) {
    }
}
