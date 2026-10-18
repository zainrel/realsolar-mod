package com.example.realsolar.mixin;

import com.example.realsolar.SolarCalculator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Shadow private int skyDarken;
    @Shadow public abstract float getRainLevel(float delta);
    @Shadow public abstract float getThunderLevel(float delta);
    @Shadow public abstract DimensionType dimensionType();
    @Shadow public abstract LevelData getLevelData();

    @Inject(method = "updateSkyBrightness", at = @At("HEAD"), cancellable = true)
    private void realsolar$updateRealisticSkyBrightness(CallbackInfo ci) {
        if (this.dimensionType().fixedTime().isPresent()) {
            return;
        }
        long dayTime = this.getLevelData().getDayTime();
        float rain = this.getRainLevel(1.0F);
        float thunder = this.getThunderLevel(1.0F);

        this.skyDarken = Mth.clamp(
                SolarCalculator.calculateSkyDarken(dayTime, rain, thunder),
                0, 11
        );
        ci.cancel();
    }
}
