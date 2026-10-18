package com.example.realsolar.client;

import com.example.realsolar.SolarConfig;
import com.example.realsolar.network.SolarSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class RealSolarClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Принимаем пакет от сервера при входе в мир или при изменении настроек админом
        ClientPlayNetworking.registerGlobalReceiver(SolarSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                SolarConfig.applySynced(
                        payload.latitudeDegrees(),
                        payload.axialTiltDegrees(),
                        payload.daysPerYear(),
                        payload.startDay(),
                        payload.realisticWinterDimming()
                );
                if (context.client().level != null) {
                    context.client().level.updateSkyBrightness();
                }
            });
        });

        // При выходе с сервера возвращаем локальные настройки из файла config/realsolar.json
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(SolarConfig::load);
        });
    }
}
