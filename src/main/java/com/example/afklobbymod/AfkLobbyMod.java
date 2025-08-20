package com.example.afklobbymod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkLobbyMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("afklobbymod");
    private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private static RegistryKey<World> lobbyWorld = null;
    private static double lobbyX = 0.0;
    private static double lobbyY = 0.0;
    private static double lobbyZ = 0.0;
    private static final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("afk_lobby_mod.json");

    @Override
    public void onInitialize() {
        loadConfig();

        ServerTickEvents.END_SERVER_TICK.register(AfkLobbyMod::onServerTickEnd);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("afk")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.literal("setlobby")
                            .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
                                    .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
                                            .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                                                    .executes(context -> {
                                                        setLobby(context.getSource(), DoubleArgumentType.getDouble(context, "x"),
                                                                DoubleArgumentType.getDouble(context, "y"),
                                                                DoubleArgumentType.getDouble(context, "z"));
                                                        return 1;
                                                    })))))
                    .then(CommandManager.literal("status")
                            .executes(context -> {
                                showStatus(context.getSource());
                                return 1;
                            })));
        });
    }

    private static void onServerTickEnd(MinecraftServer server) {
        long currentTick = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            PlayerData data = playerDataMap.computeIfAbsent(uuid, k -> new PlayerData());
            Vec3d pos = player.getPos();

            if (data.lastPos == null) {
                data.lastPos = pos;
                data.lastMoveTick = currentTick;
                continue;
            }

            boolean moved = !pos.equals(data.lastPos);

            if (data.isInLobby) {
                if (moved) {
                    data.lastPos = pos;
                    if (data.returnCountdown == null) {
                        data.returnCountdown = 60;
                    }
                }
                if (data.returnCountdown != null) {
                    if (data.returnCountdown % 20 == 0 && data.returnCountdown > 0) {
                        int secs = data.returnCountdown / 20;
                        player.sendMessage(Text.literal("Returning in " + secs + " seconds..."), false);
                    }
                    data.returnCountdown--;
                    if (data.returnCountdown <= 0) {
                        ServerWorld originalServerWorld = server.getWorld(data.originalWorld);
                        if (originalServerWorld != null) {
                            player.teleport(originalServerWorld, data.originalPos.x, data.originalPos.y, data.originalPos.z, data.originalYaw, data.originalPitch);
                        }
                        data.reset();
                    }
                }
            } else {
                if (moved) {
                    data.lastPos = pos;
                    data.lastMoveTick = currentTick;
                    if (data.afkCountdown != null) {
                        player.sendMessage(Text.literal("AFK countdown cancelled."), false);
                        data.afkCountdown = null;
                    }
                } else {
                    if (lobbyWorld != null && currentTick - data.lastMoveTick > 6000) {
                        if (data.afkCountdown == null) {
                            data.afkCountdown = 200;
                        }
                        if (data.afkCountdown % 20 == 0 && data.afkCountdown > 0) {
                            int secs = data.afkCountdown / 20;
                            player.sendMessage(Text.literal("You will be sent to AFK lobby in " + secs + " seconds. Move to cancel."), false);
                        }
                        data.afkCountdown--;
                        if (data.afkCountdown <= 0) {
                            data.originalWorld = player.getWorld().getRegistryKey();
                            data.originalPos = pos;
                            data.originalYaw = player.getYaw();
                            data.originalPitch = player.getPitch();
                            ServerWorld lobbyServerWorld = server.getWorld(lobbyWorld);
                            if (lobbyServerWorld != null) {
                                player.teleport(lobbyServerWorld, lobbyX, lobbyY, lobbyZ, data.originalYaw, data.originalPitch);
                                data.lastPos = player.getPos();
                                data.isInLobby = true;
                            }
                            data.afkCountdown = null;
                        }
                    }
                }
            }
        }
    }

    private static void setLobby(ServerCommandSource source, double x, double y, double z) {
        lobbyWorld = source.getWorld().getRegistryKey();
        lobbyX = x;
        lobbyY = y;
        lobbyZ = z;
        saveConfig();
        source.sendFeedback(() -> Text.literal("AFK lobby set to " + x + " " + y + " " + z + " in " + lobbyWorld.getValue()), false);
    }

    private static void showStatus(ServerCommandSource source) {
        if (lobbyWorld == null) {
            source.sendFeedback(() -> Text.literal("AFK lobby not set."), false);
        } else {
            source.sendFeedback(() -> Text.literal("AFK lobby: " + lobbyX + " " + lobbyY + " " + lobbyZ + " in " + lobbyWorld.getValue()), false);
        }
    }

    private static void loadConfig() {
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                lobbyWorld = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(json.get("dimension").getAsString()));
                lobbyX = json.get("x").getAsDouble();
                lobbyY = json.get("y").getAsDouble();
                lobbyZ = json.get("z").getAsDouble();
            } catch (IOException e) {
                LOGGER.warn("Failed to load AFK lobby config", e);
            }
        }
    }

    private static void saveConfig() {
        if (lobbyWorld == null) return;
        try {
            JsonObject json = new JsonObject();
            json.addProperty("dimension", lobbyWorld.getValue().toString());
            json.addProperty("x", lobbyX);
            json.addProperty("y", lobbyY);
            json.addProperty("z", lobbyZ);
            Files.writeString(configPath, json.toString());
        } catch (IOException e) {
            LOGGER.warn("Failed to save AFK lobby config", e);
        }
    }

    private static class PlayerData {
        Vec3d lastPos = null;
        long lastMoveTick = 0;
        Integer afkCountdown = null;
        Integer returnCountdown = null;
        Vec3d originalPos = null;
        RegistryKey<World> originalWorld = null;
        float originalYaw = 0f;
        float originalPitch = 0f;
        boolean isInLobby = false;

        void reset() {
            lastPos = null;
            lastMoveTick = 0;
            afkCountdown = null;
            returnCountdown = null;
            originalPos = null;
            originalWorld = null;
            originalYaw = 0f;
            originalPitch = 0f;
            isInLobby = false;
        }
    }
}