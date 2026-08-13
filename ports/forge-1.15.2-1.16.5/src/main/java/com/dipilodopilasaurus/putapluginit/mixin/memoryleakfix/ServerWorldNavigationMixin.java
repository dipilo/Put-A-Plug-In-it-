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
package com.dipilodopilasaurus.putapluginit.mixin.memoryleakfix;

import com.dipilodopilasaurus.putapluginit.extensions.ExtendDrowned;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Fixes <a href="https://bugs.mojang.com/browse/MC-202246">MC-202246</a>.
 *
 * <p>{@code ServerWorld.add} puts {@code mob.getNavigation()} into {@code navigations} at spawn and
 * {@code removeEntityComplete} takes {@code mob.getNavigation()} back out at removal. For a Drowned
 * those are different objects, because it swaps {@code navigation} between its water and ground
 * navigations while ticking, so the instance added at spawn is never removed. Each stale entry pins
 * the whole Drowned, and {@code sendBlockUpdated} walks the set on every block change.
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldNavigationMixin {
    private ServerWorldNavigationMixin() {
    }

    @Shadow
    @Final
    private Set<PathNavigator> navigations;

    @Inject(method = "onEntityRemoved", at = @At("RETURN"))
    @SuppressWarnings("java:S100") // Mixin requires the papi$ prefix to keep injected members unique.
    private void papi$removeAllDrownedNavigations(Entity entity, CallbackInfo ci) {
        if (entity instanceof ExtendDrowned) {
            ((ExtendDrowned) entity).papi$removeNavigations(this.navigations);
        }
    }
}
