/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 *
 * Adapted from MemoryLeakFix by Fx Morin (fxmorin / ca.fxco)
 * https://github.com/fxmorin/memoryLeakFix
 * Original work Copyright (C) Fx Morin. Original license: GNU LGPL-2.1-only.
 *
 * Changed by dipilo and PAPI contributors on 2026-07-19; modified through 2026.
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
package com.dipilodopilasaurus.putapluginit.extensions;

import net.minecraft.pathfinding.PathNavigator;

import java.util.Set;

/**
 * Implemented on {@code DrownedEntity} by
 * {@link com.dipilodopilasaurus.putapluginit.mixin.memoryleakfix.DrownedNavigationMixin} so the
 * {@code ServerWorld} mixin can hand the level's navigation set to the entity that knows which
 * navigations belong to it. Only meaningful on 1.16.3-1.16.5; see MC-202246.
 */
public interface ExtendDrowned {

    @SuppressWarnings("java:S100") // Mixin requires the papi$ prefix to keep injected members unique.
    void papi$removeNavigations(Set<PathNavigator> navigations);
}
