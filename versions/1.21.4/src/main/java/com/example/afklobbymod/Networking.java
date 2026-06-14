package com.example.afklobbymod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Networking {
    public static final Identifier HUD_SYNC = id("hud_sync");
    public static final Identifier TOGGLE_HUD = id("toggle_hud");
    private static final Map<UUID, Boolean> hudEnabled = new HashMap<>();

    public static Identifier id(String path) {
        return Identifier.of(AfkLobbyMod.MOD_ID, path);
    }

    public static boolean isHudEnabled(ServerPlayerEntity player) {
        return hudEnabled.getOrDefault(player.getUuid(), ModConfig.get().enableLeaderboardHud);
    }

    public static void setHudEnabled(ServerPlayerEntity player, boolean enabled) {
        hudEnabled.put(player.getUuid(), enabled);
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(HudSyncPayload.ID, HudSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleHudPayload.ID, ToggleHudPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleHudPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                setHudEnabled(player, payload.enabled());
            });
        });
    }

    public static void sendHudSync(MinecraftServer server, LeaderboardStorage leaderboard) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enableLeaderboardHud) return;
        var top = leaderboard.getTop(cfg.leaderboardSize);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new HudSyncPayload(isHudEnabled(player), top));
        }
    }

    public record HudSyncPayload(boolean enabled, List<LeaderboardStorage.LeaderboardEntry> top) implements CustomPayload {
        public static final CustomPayload.Id<HudSyncPayload> ID = new CustomPayload.Id<>(Networking.HUD_SYNC);

        public static final PacketCodec<PacketByteBuf, HudSyncPayload> CODEC = PacketCodec.of(
                Networking::writeHudSync,
                Networking::readHudSync
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ToggleHudPayload(boolean enabled) implements CustomPayload {
        public static final CustomPayload.Id<ToggleHudPayload> ID = new CustomPayload.Id<>(Networking.TOGGLE_HUD);

        public static final PacketCodec<PacketByteBuf, ToggleHudPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeBoolean(payload.enabled()),
                buf -> new ToggleHudPayload(buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private static void writeHudSync(HudSyncPayload payload, PacketByteBuf buf) {
        buf.writeBoolean(payload.enabled());
        buf.writeInt(payload.top().size());
        for (var e : payload.top()) {
            buf.writeUuid(e.uuid);
            buf.writeString(e.name);
            buf.writeLong(e.millis);
        }
    }

    private static HudSyncPayload readHudSync(PacketByteBuf buf) {
        boolean enabled = buf.readBoolean();
        int size = buf.readInt();
        LeaderboardStorage.LeaderboardEntry[] entries = new LeaderboardStorage.LeaderboardEntry[size];
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUuid();
            String name = buf.readString();
            long millis = buf.readLong();
            entries[i] = new LeaderboardStorage.LeaderboardEntry(uuid, name, millis);
        }
        return new HudSyncPayload(enabled, List.of(entries));
    }
}
