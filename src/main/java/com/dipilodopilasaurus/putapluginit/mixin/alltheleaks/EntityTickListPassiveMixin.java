/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - the technique of clearing
 * EntityTickList's "passive" backup map after iteration. See NOTICE.md.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version. See <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit.mixin.alltheleaks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Clears {@link EntityTickList}'s {@code passive} map at the end of {@code forEach}. Vanilla leaves
 * the previous tick's entity set there until the next swap, pinning removed and unloaded entities
 * for as long as nothing is added or removed.
 *
 * <p>{@code TAIL}, after the {@code finally} block nulls {@code iterated}, rather than AllTheLeaks'
 * inject at an ordinal {@code iterated} field write - stable across versions and mappings.
 */
@Mixin(EntityTickList.class)
public abstract class EntityTickListPassiveMixin {
    @Shadow
    private Int2ObjectMap<Entity> passive;

    @Inject(method = "forEach", at = @At("TAIL"))
    @SuppressWarnings("java:S100") // Mixin requires the papi$ prefix to keep injected members unique.
    private void papi$clearPassiveAfterTick(Consumer<Entity> action, CallbackInfo ci) {
        this.passive.clear();
    }
}
