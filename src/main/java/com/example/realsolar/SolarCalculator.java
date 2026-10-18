package com.example.realsolar;

import net.minecraft.util.Mth;

public class SolarCalculator {
    public static final long TICKS_PER_DAY = 24000L;

    /**
     * Возвращает мировое время со сдвигом на выбранный начальный день года (startDay от 1 до 28).
     * Если startDay = 1, сдвиг равен 0.
     * Если startDay = 8 (лето), то уже в 1-й день мира (worldTime = 0) в игре будет 8-й день календаря (солнце 60°).
     */
    public static long getSeasonAdjustedTime(long worldTime) {
        int clampedStartDay = Math.clamp(SolarConfig.startDay, 1, Math.max(1, SolarConfig.daysPerYear));
        long offsetTicks = (long) (clampedStartDay - 1) * TICKS_PER_DAY;
        return worldTime + offsetTicks;
    }

    /**
     * Текущий день года (от 1 до daysPerYear, по умолчанию 1..28) с учетом выбранного startDay.
     */
    public static int getCurrentDayOfYear(long worldTime) {
        long adjustedTime = getSeasonAdjustedTime(worldTime);
        return (int) Math.floorMod(adjustedTime / TICKS_PER_DAY, (long) SolarConfig.daysPerYear) + 1;
    }

    /**
     * Склонение солнца (delta) в радианах.
     * Плавно меняется каждый день (и каждый тик) в течение 28-дневного года с учетом startDay.
     */
    public static double getSolarDeclinationRad(long worldTime) {
        long adjustedTime = getSeasonAdjustedTime(worldTime);
        long ticksPerYear = Math.max(1L, (long) SolarConfig.daysPerYear * TICKS_PER_DAY);
        double yearPhase = (double) Math.floorMod(adjustedTime, ticksPerYear) / (double) ticksPerYear;
        double maxTiltRad = Math.toRadians(SolarConfig.axialTiltDegrees);
        return maxTiltRad * Math.sin(yearPhase * 2.0 * Math.PI);
    }

    /**
     * Часовой угол солнца (H) в радианах:
     * 0 в полдень (6000 тиков), -PI/2 на рассвете (0 тиков), +PI/2 на закате (12000 тиков), +-PI в полночь (18000 тиков).
     */
    public static double getHourAngleRad(long worldTime) {
        long timeOfDay = Math.floorMod(worldTime, TICKS_PER_DAY);
        double normalized = ((double) (timeOfDay - 6000L) / (double) TICKS_PER_DAY);
        return normalized * 2.0 * Math.PI;
    }

    /**
     * Синус текущего угла высоты солнца над горизонтом:
     * sin(alpha) = sin(phi)*sin(delta) + cos(phi)*cos(delta)*cos(H)
     */
    public static double getSinSolarElevation(long worldTime) {
        double phi = Math.toRadians(SolarConfig.latitudeDegrees);
        double delta = getSolarDeclinationRad(worldTime);
        double H = getHourAngleRad(worldTime);

        return Math.sin(phi) * Math.sin(delta) + Math.cos(phi) * Math.cos(delta) * Math.cos(H);
    }

    /**
     * Текущая высота солнца над горизонтом в градусах (-90° .. +90°).
     */
    public static double getSolarElevationDegrees(long worldTime) {
        double sinAlpha = Mth.clamp(getSinSolarElevation(worldTime), -1.0, 1.0);
        return Math.toDegrees(Math.asin(sinAlpha));
    }

    /**
     * Максимальная высота солнца в полдень текущего дня (в градусах).
     * На широте 52.5° при наклоне 22.5°:
     * - День 8 (Пик лета): 60.0°
     * - Дни 1 и 15 (Весна/Осень): 37.5°
     * - День 22 (Пик зимы): 15.0°
     */
    public static double getNoonMaxElevationDegrees(long worldTime) {
        double phi = SolarConfig.latitudeDegrees;
        double deltaDeg = Math.toDegrees(getSolarDeclinationRad(worldTime));
        double sinNoon = Math.sin(Math.toRadians(phi)) * Math.sin(Math.toRadians(deltaDeg))
                       + Math.cos(Math.toRadians(phi)) * Math.cos(Math.toRadians(deltaDeg));
        return Math.toDegrees(Math.asin(Mth.clamp(sinNoon, -1.0, 1.0)));
    }

    /**
     * Длительность светового дня в часах (от 0.0 ч в полярную ночь до 24.0 ч в полярный день).
     */
    public static double getDaylightHours(long worldTime) {
        double phi = Math.toRadians(SolarConfig.latitudeDegrees);
        double delta = getSolarDeclinationRad(worldTime);
        double cosH0 = -Math.tan(phi) * Math.tan(delta);

        if (cosH0 <= -1.0) return 24.0; // Полярный день (солнце не заходит)
        if (cosH0 >= 1.0) return 0.0;   // Полярная ночь (солнце не восходит)
        return (2.0 * Math.toDegrees(Math.acos(cosH0))) / 15.0;
    }

    /**
     * Перевод реальной высоты солнца в небесный угол Minecraft:
     * 0.0 = полдень, 0.25 = закат, 0.5 = полночь, 0.75 = рассвет.
     */
    public static float getRealisticCelestialAngle(long worldTime) {
        double sinAlpha = Mth.clamp(getSinSolarElevation(worldTime), -1.0, 1.0);
        long timeOfDay = Math.floorMod(worldTime, TICKS_PER_DAY);
        boolean afternoon = timeOfDay >= 6000L && timeOfDay < 18000L;

        double elevationRatio = Math.asin(sinAlpha) / (Math.PI / 2.0); // от -1.0 до +1.0

        float angle;
        if (elevationRatio >= 0.0) {
            float dayOffset = (float) ((1.0 - elevationRatio) * 0.25);
            angle = afternoon ? dayOffset : (1.0F - dayOffset);
        } else {
            float nightOffset = (float) ((-elevationRatio) * 0.25);
            angle = afternoon ? (0.25F + nightOffset) : (0.75F - nightOffset);
        }
        return Mth.positiveModulo(angle, 1.0F);
    }

    /**
     * Расчет глобального затемнения неба (skyDarken: от 0 до 11).
     * Напрямую определяет уровень освещенности в мире и условие спавна враждебных мобов!
     */
    public static int calculateSkyDarken(long worldTime, float rainLevel, float thunderLevel) {
        double sinAlpha = getSinSolarElevation(worldTime);

        double brightnessFactor;
        if (SolarConfig.realisticWinterDimming) {
            brightnessFactor = Mth.clamp((sinAlpha + 0.12) / 0.82, 0.0, 1.0);
        } else {
            brightnessFactor = Mth.clamp((sinAlpha + 0.15) / 0.35, 0.0, 1.0);
        }

        brightnessFactor *= (1.0 - (rainLevel * 5.0F / 16.0F));
        brightnessFactor *= (1.0 - (thunderLevel * 5.0F / 16.0F));

        return (int) Math.round((1.0 - brightnessFactor) * 11.0);
    }
}
