package com.example.realsolar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class SolarConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("realsolar.json");

    // Широта в градусах:
    // 52.5° дает ровно 60° максимум солнца летом (день 8) и 15° максимум солнца зимой (день 22).
    // Широта >= 67.5° включает Заполярье (полярный день летом и полярная ночь зимой).
    public static double latitudeDegrees = 52.5;

    // Наклон оси планеты в градусах (22.5° в паре с широтой 52.5° дает диапазон 15°..60°)
    public static double axialTiltDegrees = 22.5;

    // Длина года в игровых днях (28 дней = 4 сезона по 7 дней)
    public static int daysPerYear = 28;

    // Начальный день года при старте мира (от 1 до 28):
    // 1 = Начало Весны (равноденствие, 37.5°)
    // 8 = Пик Лета (солнце 60°)
    // 15 = Начало Осени (равноденствие, 37.5°)
    // 22 = Пик Зимы (солнце 15°)
    public static int startDay = 1;

    // Реалистичное затемнение освещения зимой (влияет на спавн мобов при низком солнце)
    public static boolean realisticWinterDimming = true;

    private static class ConfigData {
        double latitudeDegrees = 52.5;
        double axialTiltDegrees = 22.5;
        int daysPerYear = 28;
        int startDay = 1;
        boolean realisticWinterDimming = true;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                latitudeDegrees = Math.clamp(data.latitudeDegrees, -90.0, 90.0);
                axialTiltDegrees = Math.clamp(data.axialTiltDegrees, 0.0, 45.0);
                daysPerYear = Math.max(4, data.daysPerYear);
                startDay = Math.clamp(data.startDay, 1, daysPerYear);
                realisticWinterDimming = data.realisticWinterDimming;
            }
        } catch (IOException e) {
            System.err.println("[RealSolar] Не удалось прочитать config/realsolar.json: " + e.getMessage());
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.latitudeDegrees = latitudeDegrees;
        data.axialTiltDegrees = axialTiltDegrees;
        data.daysPerYear = daysPerYear;
        data.startDay = Math.clamp(startDay, 1, daysPerYear);
        data.realisticWinterDimming = realisticWinterDimming;
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[RealSolar] Не удалось сохранить config/realsolar.json: " + e.getMessage());
        }
    }

    /**
     * Применяет настройки, полученные от сервера по сети (в онлайне),
     * не перезаписывая локальный файл конфигурации игрока.
     */
    public static void applySynced(double lat, double tilt, int days, int start, boolean winterDim) {
        latitudeDegrees = Math.clamp(lat, -90.0, 90.0);
        axialTiltDegrees = Math.clamp(tilt, 0.0, 45.0);
        daysPerYear = Math.max(4, days);
        startDay = Math.clamp(start, 1, daysPerYear);
        realisticWinterDimming = winterDim;
    }
}
