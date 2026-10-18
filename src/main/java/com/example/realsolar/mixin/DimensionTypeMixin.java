package com.example.realsolar.mixin;

import com.example.realsolar.SolarCalculator;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;

@Mixin(DimensionType.class)
public abstract class DimensionTypeMixin {
    @Shadow public abstract OptionalLong fixedTime();

    @Inject(method = "timeOfDay", at = @At("HEAD"), cancellable = true)
    private void realsolar$overrideTimeOfDay(long dayTime, CallbackInfoReturnable<Float> cir) {
        if (this.fixedTime().isPresent()) {
            return;
        }
        cir.setReturnValue(SolarCalculator.getRealisticCelestialAngle(dayTime));
    }
}
