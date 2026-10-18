package com.example.realsolar;

import com.example.realsolar.network.SolarSyncPayload;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class RealSolarMod implements ModInitializer {
    public static final String MOD_ID = "realsolar";

    @Override
    public void onInitialize() {
        SolarConfig.load();

        // 1. Регистрация сетевого пакета синхронизации Сервер -> Клиент (S2C)
        PayloadTypeRegistry.playS2C().register(SolarSyncPayload.TYPE, SolarSyncPayload.CODEC);

        // 2. Автоматическая отправка настроек сервера каждому игроку при подключении к миру/серверу
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sender.sendPacket(SolarSyncPayload.fromCurrentConfig());
        });

        // 3. Команды управления (/solar info, /solar startday, /solar latitude, /solar preset)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("solar")
                .then(Commands.literal("info").executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    long time = level.getDayTime();
                    int dayOfYear = SolarCalculator.getCurrentDayOfYear(time);
                    int seasonIndex = ((dayOfYear - 1) * 4) / SolarConfig.daysPerYear;
                    String[] seasons = {"Весна (Дни 1-7)", "Лето (Дни 8-14)", "Осень (Дни 15-21)", "Зима (Дни 22-28)"};
                    String season = seasons[Math.clamp(seasonIndex, 0, 3)];

                    double currentAlt = SolarCalculator.getSolarElevationDegrees(time);
                    double maxAlt = SolarCalculator.getNoonMaxElevationDegrees(time);
                    double dayHours = SolarCalculator.getDaylightHours(time);
                    int skyDarken = level.getSkyDarken();

                    boolean isPolar = Math.abs(SolarConfig.latitudeDegrees) >= (90.0 - SolarConfig.axialTiltDegrees);
                    String zoneName = isPolar ? "Заполярье (Полярный круг)" : "Умеренный/Южный пояс";

                    ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                            "§6=== Реалистичное Солнце (26.3 Online Sync) ===\n" +
                            "§eТекущий день года: §f%d из %d (%s) §7[Начальный день: %d]\n" +
                            "§eШирота: §f%.1f° (%s)\n" +
                            "§eВысота солнца сейчас: §f%.1f° §7(Максимум сегодня: %.1f°)\n" +
                            "§eДлина светового дня: §f%.1f ч. из 24 ч.\n" +
                            "§eСвет неба: §f%d/15 §7(skyDarken: %d)",
                            dayOfYear, SolarConfig.daysPerYear, season, SolarConfig.startDay,
                            SolarConfig.latitudeDegrees, zoneName,
                            currentAlt, maxAlt, dayHours, 15 - skyDarken, skyDarken
                    )), false);
                    return 1;
                }))
                .then(Commands.literal("startday")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("day", IntegerArgumentType.integer(1, 28))
                        .executes(ctx -> {
                            int startDay = IntegerArgumentType.getInteger(ctx, "day");
                            SolarConfig.startDay = Math.clamp(startDay, 1, SolarConfig.daysPerYear);
                            SolarConfig.save();
                            applyAndSync(ctx.getSource().getServer());

                            int currentDay = SolarCalculator.getCurrentDayOfYear(ctx.getSource().getLevel().getDayTime());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§a[RealSolar] Начальный день установлен на " + SolarConfig.startDay +
                                    " (текущий день в мире теперь: " + currentDay + " из " + SolarConfig.daysPerYear + "). Синхронизировано со всеми игроками!"
                            ), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("latitude")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("degrees", DoubleArgumentType.doubleArg(-90.0, 90.0))
                        .executes(ctx -> {
                            double lat = DoubleArgumentType.getDouble(ctx, "degrees");
                            SolarConfig.latitudeDegrees = lat;
                            SolarConfig.save();
                            applyAndSync(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§a[RealSolar] Широта установлена на " + lat + "° и синхронизирована со всеми игроками онлайн!"
                            ), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("preset")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("temperate").executes(ctx -> {
                        SolarConfig.latitudeDegrees = 52.5;
                        SolarConfig.save();
                        applyAndSync(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§a[RealSolar] Включен умеренный пояс (52.5°): солнце до 60° летом и до 15° зимой (синхронизировано)."
                        ), true);
                        return 1;
                    }))
                    .then(Commands.literal("polar").executes(ctx -> {
                        SolarConfig.latitudeDegrees = 72.0;
                        SolarConfig.save();
                        applyAndSync(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§b[RealSolar] Включено Заполярье (72.0°): полярный день летом и полярная ночь зимой (синхронизировано)!"
                        ), true);
                        return 1;
                    }))
                    .then(Commands.literal("equator").executes(ctx -> {
                        SolarConfig.latitudeDegrees = 0.0;
                        SolarConfig.save();
                        applyAndSync(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§e[RealSolar] Включен Экватор (0.0°): день равен ночи круглый год (синхронизировано)."
                        ), true);
                        return 1;
                    }))
                )
            );
        });
    }

    /**
     * Обновляет освещение на сервере и рассылает пакет синхронизации всем игрокам онлайн.
     */
    public static void applyAndSync(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            level.updateSkyBrightness();
        }
        SolarSyncPayload payload = SolarSyncPayload.fromCurrentConfig();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
