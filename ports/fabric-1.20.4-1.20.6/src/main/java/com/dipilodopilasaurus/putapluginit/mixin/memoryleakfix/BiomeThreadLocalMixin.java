package com.dipilodopilasaurus.putapluginit.mixin.memoryleakfix;

import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(Biome.class)
public abstract class BiomeThreadLocalMixin {
    @Unique
    private static ThreadLocal<Long2FloatLinkedOpenHashMap> papiSharedTemperatureCache;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/ThreadLocal;withInitial(Ljava/util/function/Supplier;)Ljava/lang/ThreadLocal;"
            ),
            require = 0
    )
    private ThreadLocal<Long2FloatLinkedOpenHashMap> papiUseSharedThreadLocal(Supplier supplier) {
        if (papiSharedTemperatureCache == null) {
            papiSharedTemperatureCache = ThreadLocal.withInitial(supplier);
        }
        return papiSharedTemperatureCache;
    }
}