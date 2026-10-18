package com.example.realsolar.mixin.client;

import com.example.realsolar.SolarCalculator;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @ModifyArg(
        method = "renderSunMoonAndStars",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
            ordinal = 0
        ),
        index = 0
    )
    private Quaternionf realsolar$modifySunRotation(Quaternionf vanillaTimeRotation) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return vanillaTimeRotation;
        }

        long dayTime = mc.level.getDayTime();
        float maxNoonElevation = (float) SolarCalculator.getNoonMaxElevationDegrees(dayTime);
        float zenithTiltDegrees = 90.0F - maxNoonElevation;

        Quaternionf seasonalTilt = new Quaternionf(Axis.XP.rotationDegrees(zenithTiltDegrees));
        seasonalTilt.mul(vanillaTimeRotation);
        return seasonalTilt;
    }
}
