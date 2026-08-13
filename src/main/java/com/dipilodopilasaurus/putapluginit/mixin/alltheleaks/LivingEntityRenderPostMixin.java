/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - the technique of clearing the
 * per-render state that Entity Model Features and Entity Texture Features leave
 * on the renderer after each living-entity render. See NOTICE.md.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version. See <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit.mixin.alltheleaks;

import com.dipilodopilasaurus.putapluginit.modfix.ModFixes;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Post-render hook for the EMF/ETF fixes. Both mods add instance fields to the renderer that hold
 * the entity (or its animation context) rendered last, and neither always clears them, so the last
 * entity rendered by each renderer is pinned until something else is rendered through it.
 *
 * <p>A Mixin because AllTheLeaks' {@code RenderLivingEvent.Post} has no loader-agnostic equivalent.
 * Injects at {@code RETURN}, not {@code TAIL}, to cover the early-return paths, and the handler takes
 * only the {@code CallbackInfo} so it survives the 1.21.5 render-state signature refactor.
 *
 * <p>1.21.9 renamed {@code render} to {@code submit}, so the target is gated rather than listed
 * twice - a dead name on any node makes the Forge annotation processor report "unable to determine
 * descriptor". No node's range spans the rename, and neither name carries a descriptor.
 *
 * <p>{@code PapiMixinConfigPlugin} skips this mixin unless EMF or ETF is installed - it is the one
 * PAPI injection in a per-entity, per-frame path.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRenderPostMixin {
    //? if >=1.21.9 {
    /*@Inject(method = "submit", at = @At("RETURN"), require = 0)
    *///?} else {
    @Inject(method = "render", at = @At("RETURN"), require = 0)
    //?}
    @SuppressWarnings("java:S100") // Mixin requires the papi$ prefix to keep injected members unique.
    private void papi$clearRendererState(CallbackInfo ci) {
        ModFixes.onLivingEntityRenderPost(this);
    }
}
