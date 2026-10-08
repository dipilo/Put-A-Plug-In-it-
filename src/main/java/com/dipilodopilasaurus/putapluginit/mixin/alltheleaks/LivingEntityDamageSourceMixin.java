/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - the technique of expiring a
 * LivingEntity's retained last-damage source from its tick. See NOTICE.md.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version. See <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit.mixin.alltheleaks;

import com.dipilodopilasaurus.putapluginit.Config;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops a {@link LivingEntity}'s {@code lastDamageSource} once it has expired. Vanilla keeps it
 * until something reads it, so a mob hurt once and never read again pins its attacker - and that
 * attacker's level - for the rest of its life.
 *
 * <p>AllTheLeaks calls the getter from a NeoForge entity-tick event, which clears the field as a
 * side effect. 26.3 refactored the getter to stop clearing, so the write is explicit here and the
 * fix needs no version gate.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageSourceMixin {
    @Shadow
    private DamageSource lastDamageSource;

    @Shadow
    public abstract DamageSource getLastDamageSource();

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    @SuppressWarnings("java:S100") // Mixin requires the papi$ prefix to keep injected members unique.
    private void papi$expireLastDamageSource(CallbackInfo ci) {
        // The null check is the whole cost on the overwhelming majority of ticks.
        if (this.lastDamageSource != null && Config.isExpireLastDamageSourceEnabled()
                && getLastDamageSource() == null) {
            this.lastDamageSource = null;
        }
    }
}
