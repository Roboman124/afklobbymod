package com.example.afklobbymod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Networking {
    public static final Identifier HUD_SYNC = id("hud_sync");
    public static final Identifier TOGGLE_HUD = id("toggle_hud");
    private static final Map<UUID, Boolean> hudEnabled = new HashMap<>();

    public static Identifier id(String path) {
        return new Identifier(AfkLobbyMod.MOD_ID, path);
    }

    public static boolean isHudEnabled(ServerPlayerEntity player) {
        return hudEnabled.getOrDefault(player.getUuid(), ModConfig.get().enableLeaderboardHud);
    }

    public static void setHudEnabled(ServerPlayerEntity player, boolean enabled) {
        hudEnabled.put(player.getUuid(), enabled);
    }

    public static void sendHudSync(MinecraftServer server, LeaderboardStorage leaderboard) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enableLeaderboardHud) return;
        var top = leaderboard.getTop(cfg.leaderboardSize);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(isHudEnabled(player));
            buf.writeInt(top.size());
            for (var e : top) {
                buf.writeUuid(e.uuid);
                buf.writeString(e.name);
                buf.writeLong(e.millis);
            }
            ServerPlayNetworking.send(player, HUD_SYNC, buf);
        }
    }
}
