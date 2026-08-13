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

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import net.minecraft.tags.TagKey;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class TagKeyLeakFix {

    private static boolean attempted;

    private TagKeyLeakFix() {
    }

    @SuppressWarnings("java:S3011") // Swapping TagKey's strong interner for a weak one requires field access.
    public static void applyIfNeeded() {
        if (attempted) {
            return;
        }
        attempted = true;

        if (!McVersion.is("1.18.2")) {
            return;
        }

        try {
            Field internerField = findStaticInternerField(TagKey.class);
            if (internerField == null) {
                return;
            }

            internerField.setAccessible(true);
            removeFinalModifier(internerField);
            internerField.set(null, Interners.newWeakInterner());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Best-effort: if internals differ for a patch, skip silently.
        }
    }

    private static Field findStaticInternerField(Class<?> tagKeyClass) {
        for (Field field : tagKeyClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Interner.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }

    @SuppressWarnings("java:S3011")
    private static void removeFinalModifier(Field field) {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (ReflectiveOperationException ignored) {
            // Java version dependent; if unavailable, leave as-is and try direct set.
        }
    }
}