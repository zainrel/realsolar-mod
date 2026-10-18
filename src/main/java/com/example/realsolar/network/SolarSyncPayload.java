package com.example.realsolar.network;

import com.example.realsolar.RealSolarMod;
import com.example.realsolar.SolarConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Сетевой пакет синхронизации настроек солнца от сервера к клиенту (S2C).
 * Гарантирует, что у всех игроков на сервере одинаковая широта, начальный день (1-28),
 * длина дня, освещенность и 3D-наклон солнца на небе.
 */
public record SolarSyncPayload(
        double latitudeDegrees,
        double axialTiltDegrees,
        int daysPerYear,
        int startDay,
        boolean realisticWinterDimming
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SolarSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RealSolarMod.MOD_ID, "sync_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SolarSyncPayload> CODEC =
            StreamCodec.ofMember(SolarSyncPayload::write, SolarSyncPayload::new);

    public SolarSyncPayload(RegistryFriendlyByteBuf buf) {
        this(
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(this.latitudeDegrees);
        buf.writeDouble(this.axialTiltDegrees);
        buf.writeVarInt(this.daysPerYear);
        buf.writeVarInt(this.startDay);
        buf.writeBoolean(this.realisticWinterDimming);
    }

    public static SolarSyncPayload fromCurrentConfig() {
        return new SolarSyncPayload(
                SolarConfig.latitudeDegrees,
                SolarConfig.axialTiltDegrees,
                SolarConfig.daysPerYear,
                SolarConfig.startDay,
                SolarConfig.realisticWinterDimming
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
