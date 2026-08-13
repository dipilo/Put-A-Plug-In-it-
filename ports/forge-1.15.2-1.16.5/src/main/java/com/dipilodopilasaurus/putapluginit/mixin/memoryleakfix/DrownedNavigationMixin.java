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
import net.minecraft.entity.monster.DrownedEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Half of the MC-202246 fix; see {@link ServerWorldNavigationMixin} for the leak itself.
 *
 * <p>A Drowned owns three {@code PathNavigator} instances: the one {@code MobEntity}'s constructor
 * builds via {@code createNavigation}, plus the water and ground navigations it swaps between while
 * ticking. {@code ServerWorld} only ever tracks whichever one is current, so this hands back all
 * three and lets the level drop them all.
 */
@Mixin(DrownedEntity.class)
public abstract class DrownedNavigationMixin extends ZombieEntity implements ExtendDrowned {

    @Shadow
    @Final
    protected SwimmerPathNavigator waterNavigation;

    @Shadow
    @Final
    protected GroundPathNavigator groundNavigation;

    @Unique
    private PathNavigator papi$originalNavigation;

    // Never invoked; javac only needs it because the mixin extends the target's superclass.
    private DrownedNavigationMixin(World level) {
        super(level);
    }

    @Override
    @SuppressWarnings("java:S100") // Mixin requires the papi$ prefix to keep injected members unique.
    public void papi$removeNavigations(Set<PathNavigator> navigations) {
        navigations.remove(this.papi$originalNavigation);
        navigations.remove(this.waterNavigation);
        navigations.remove(this.groundNavigation);
    }

    /**
     * Captured at construction because the swap in {@code updateSwimming} overwrites
     * {@code navigation} before the entity is ever removed, leaving no other way back to it.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    @SuppressWarnings("java:S100") // Mixin requires the papi$ prefix to keep injected members unique.
    private void papi$captureOriginalNavigation(CallbackInfo ci) {
        this.papi$originalNavigation = this.getNavigation();
    }
}
