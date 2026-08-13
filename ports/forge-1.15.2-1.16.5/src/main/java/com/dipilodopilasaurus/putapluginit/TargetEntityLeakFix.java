/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 *
 * Adapted from MemoryLeakFix by Fx Morin (fxmorin / ca.fxco)
 * https://github.com/fxmorin/memoryLeakFix
 * Original work Copyright (C) Fx Morin. Original license: GNU LGPL-2.1-only.
 *
 * Changed by dipilo and PAPI contributors on 2026-03-08; modified through 2026.
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

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/**
 * Nulls {@code Minecraft.crosshairPickEntity} and {@code Minecraft.hitResult} once the client is
 * out of its level, so the last entity looked at - and through it the whole {@code ClientLevel} -
 * is not pinned for as long as the player sits at the main menu.
 *
 * <p>The clear is confined to that out-of-level window on purpose. {@code GameRenderer#pick} is
 * the only writer of either field and it rewrites both early in every tick that has a level and a
 * player, so clearing them in-world frees nothing; all it does is hand a null to every mod that
 * reads {@code hitResult} from its own end-of-tick handler, which is the same hook PAPI clears
 * from. Create's super-glue selection is one such reader, and the null turned its right-click into
 * a no-op. See the tree copy of this class for the full account.
 *
 * <p>The SRG ids are what this island actually runs: the compile-time names here are mapped, but
 * the string passed to {@code getDeclaredField} is not, so the mojmap name alone finds nothing.
 * The ids below are read from the 1.16.5 MCP-to-SRG table; a name that does not exist on an older
 * island version is simply skipped by {@link #findField}. The {@code level} and {@code player}
 * reads need no such list - they are compiled references, which ForgeGradle remaps.
 */
public final class TargetEntityLeakFix {
    private static final Field TARGET_ENTITY_FIELD = findField("crosshairPickEntity", "field_147125_j", "pointedEntity");
    private static final Field HIT_RESULT_FIELD = findField("hitResult", "field_71476_x", "objectMouseOver");

    private TargetEntityLeakFix() {
    }

    public static void onClientTickEnd() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || isPickRefreshing(minecraft)) {
            return;
        }
        clearField(TARGET_ENTITY_FIELD, minecraft);
        clearField(HIT_RESULT_FIELD, minecraft);
    }

    /** The condition {@code GameRenderer#pick} itself tests before rewriting both fields. */
    private static boolean isPickRefreshing(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null;
    }

    @SuppressWarnings("java:S3011") // Clearing private Minecraft fields is the point of this fix.
    private static Field findField(String... names) {
        for (String name : names) {
            try {
                Field field = Minecraft.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Name differs across mappings; try the next candidate.
            }
        }
        return null;
    }

    @SuppressWarnings("java:S3011") // See findField.
    private static void clearField(Field field, Object target) {
        if (field == null) {
            return;
        }
        try {
            field.set(target, null);
        } catch (IllegalAccessException ignored) {
            // Best-effort: a locked-down field just stays uncleared.
        }
    }
}