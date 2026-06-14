package com.example.afklobbymod;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.Set;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LobbyManager {
    private final Path lobbyConfigPath = AfkLobbyModBase.configDir().resolve("afklobbymod_lobby.json");
    private String lobbyWorldId = null;
    private double lobbyX = 0, lobbyY = 64, lobbyZ = 0;
    private String stageWorldId = null;
    private double stageX = 0, stageY = 64, stageZ = 0;
    private MinecraftServer server;

    public void load(MinecraftServer server) {
        this.server = server;
        if (!Files.exists(lobbyConfigPath)) return;
        try {
            JsonObject json = ModConfig.GSON.fromJson(Files.readString(lobbyConfigPath), JsonObject.class);
            if (json == null) return;
            if (json.has("lobbyWorld")) lobbyWorldId = json.get("lobbyWorld").getAsString();
            if (json.has("lobbyX")) lobbyX = json.get("lobbyX").getAsDouble();
            if (json.has("lobbyY")) lobbyY = json.get("lobbyY").getAsDouble();
            if (json.has("lobbyZ")) lobbyZ = json.get("lobbyZ").getAsDouble();
            if (json.has("stageWorld")) stageWorldId = json.get("stageWorld").getAsString();
            if (json.has("stageX")) stageX = json.get("stageX").getAsDouble();
            if (json.has("stageY")) stageY = json.get("stageY").getAsDouble();
            if (json.has("stageZ")) stageZ = json.get("stageZ").getAsDouble();
        } catch (IOException e) {
            AfkLobbyMod.LOGGER.warn("Failed to load lobby config", e);
        }
    }

    public void save() {
        try {
            JsonObject json = new JsonObject();
            if (lobbyWorldId != null) json.addProperty("lobbyWorld", lobbyWorldId);
            json.addProperty("lobbyX", lobbyX); json.addProperty("lobbyY", lobbyY); json.addProperty("lobbyZ", lobbyZ);
            if (stageWorldId != null) json.addProperty("stageWorld", stageWorldId);
            json.addProperty("stageX", stageX); json.addProperty("stageY", stageY); json.addProperty("stageZ", stageZ);
            Files.createDirectories(lobbyConfigPath.getParent());
            Files.writeString(lobbyConfigPath, ModConfig.GSON.toJson(json));
        } catch (IOException e) {
            AfkLobbyMod.LOGGER.warn("Failed to save lobby config", e);
        }
    }

    public void setLobby(ServerWorld world, double x, double y, double z) {
        lobbyWorldId = world.getRegistryKey().getValue().toString();
        lobbyX = x; lobbyY = y; lobbyZ = z;
        save();
    }

    public void setStage(ServerWorld world, double x, double y, double z) {
        stageWorldId = world.getRegistryKey().getValue().toString();
        stageX = x; stageY = y; stageZ = z;
        save();
    }

    public boolean sendToLobby(ServerPlayerEntity player, PlayerAfkState state) {
        ServerWorld lobbyWorld = resolveWorld(lobbyWorldId);
        if (lobbyWorld == null) {
            player.sendMessage(net.minecraft.text.Text.of("AFK lobby is not set."), false);
            return false;
        }
        state.originalWorldId = player.getWorld().getRegistryKey().getValue().toString();
        state.originalX = player.getX(); state.originalY = player.getY(); state.originalZ = player.getZ();
        state.originalYaw = player.getYaw(); state.originalPitch = player.getPitch();
        player.teleport(lobbyWorld, lobbyX, lobbyY, lobbyZ, Set.of(PositionFlag.X, PositionFlag.Y, PositionFlag.Z, PositionFlag.Y_ROT, PositionFlag.X_ROT), state.originalYaw, state.originalPitch, true);
        state.isInLobby = true;
        state.markAfk();
        player.sendMessage(net.minecraft.text.Text.of("You are AFK and have been moved to the lobby."), false);
        return true;
    }

    public boolean returnPlayer(ServerPlayerEntity player, PlayerAfkState state) {
        ServerWorld original = resolveWorld(state.originalWorldId);
        if (original == null) {
            original = server.getOverworld();
        }
        player.teleport(original, state.originalX, state.originalY, state.originalZ, Set.of(PositionFlag.X, PositionFlag.Y, PositionFlag.Z, PositionFlag.Y_ROT, PositionFlag.X_ROT), state.originalYaw, state.originalPitch, true);
        state.resetReturn();
        player.sendMessage(net.minecraft.text.Text.of("Welcome back!"), false);
        return true;
    }

    public boolean teleportToStage(ServerPlayerEntity player) {
        ServerWorld stageWorld = resolveWorld(stageWorldId);
        if (stageWorld == null) stageWorld = server.getOverworld();
        player.teleport(stageWorld, stageX, stageY, stageZ, Set.of(PositionFlag.X, PositionFlag.Y, PositionFlag.Z, PositionFlag.Y_ROT, PositionFlag.X_ROT), player.getYaw(), player.getPitch(), true);
        return true;
    }

    public ServerWorld resolveWorld(String id) {
        if (id == null || server == null) return null;
        for (ServerWorld w : server.getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(id)) return w;
        }
        return null;
    }

    public String getLobbyWorldId() { return lobbyWorldId; }
    public Vec3d getLobbyPos() { return new Vec3d(lobbyX, lobbyY, lobbyZ); }
    public String getStageWorldId() { return stageWorldId; }
    public Vec3d getStagePos() { return new Vec3d(stageX, stageY, stageZ); }
}
