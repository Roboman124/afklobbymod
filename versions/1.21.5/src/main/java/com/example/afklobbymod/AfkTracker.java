package com.example.afklobbymod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkTracker {
    private final LobbyManager lobby;
    private final LeaderboardStorage leaderboard;
    private final Map<UUID, PlayerAfkState> states = new ConcurrentHashMap<>();

    public AfkTracker(LobbyManager lobby, LeaderboardStorage leaderboard) {
        this.lobby = lobby;
        this.leaderboard = leaderboard;
    }

    public void onJoin(ServerPlayerEntity player) {
        states.computeIfAbsent(player.getUuid(), u -> new PlayerAfkState(u, player.getName().getString()));
    }

    public void onLeave(ServerPlayerEntity player) {
        PlayerAfkState state = states.get(player.getUuid());
        if (state != null && state.isAfk) {
            state.totalAfkMillis += System.currentTimeMillis() - state.currentAfkStartMillis;
            leaderboard.addTime(player.getUuid(), player.getName().getString(), state.getCurrentAfkMillis());
        }
    }

    public void onInteract(ServerPlayerEntity player) { markActive(player); }
    public void onChat(ServerPlayerEntity player) { markActive(player); }
    public void onCommand(ServerPlayerEntity player) { markActive(player); }
    public void onUseItem(ServerPlayerEntity player) { markActive(player); }

    private void markActive(ServerPlayerEntity player) {
        PlayerAfkState state = states.get(player.getUuid());
        if (state == null) return;
        state.markActive(player.server.getTicks());
        if (state.isInLobby && ModConfig.get().returnOnActivity) {
            if (state.returnCountdown == null) {
                state.returnCountdown = ModConfig.get().returnCountdownSeconds * 20;
            }
        }
    }

    public void tick(MinecraftServer server) {
        long currentTick = server.getTicks();
        ModConfig cfg = ModConfig.get();
        long afkTimeoutTicks = cfg.afkTimeoutSeconds * 20L;
        long warningTicks = cfg.afkWarningSeconds * 20L;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerAfkState state = states.computeIfAbsent(player.getUuid(), u -> new PlayerAfkState(u, player.getName().getString()));
            if (state.name == null || !state.name.equals(player.getName().getString())) state.name = player.getName().getString();

            updateMovement(player, currentTick);

            if (state.isInLobby) {
                handleReturn(player, state, currentTick);
                continue;
            }

            long idleTicks = currentTick - state.lastActivityTick;
            if (idleTicks >= afkTimeoutTicks) {
                if (state.afkCountdown == null) state.afkCountdown = (int) warningTicks;
                if (state.afkCountdown % 20 == 0 && state.afkCountdown > 0) {
                    player.sendMessage(net.minecraft.text.Text.of("AFK in " + (state.afkCountdown / 20) + "s. Move/chat/interact to cancel."), false);
                }
                state.afkCountdown--;
                if (state.afkCountdown <= 0) {
                    state.markAfk();
                    if (cfg.teleportToLobby) lobby.sendToLobby(player, state);
                    leaderboard.addTime(player.getUuid(), state.name, state.getCurrentAfkMillis());
                    state.afkCountdown = null;
                }
            }
        }
    }

    private void updateMovement(ServerPlayerEntity player, long currentTick) {
        PlayerAfkState state = states.get(player.getUuid());
        if (state == null) return;
        double x = player.getX(), y = player.getY(), z = player.getZ();
        float yaw = player.getYaw(), pitch = player.getPitch();
        boolean moved = x != state.lastX || y != state.lastY || z != state.lastZ;
        boolean rotated = yaw != state.lastYaw || pitch != state.lastPitch;
        boolean ridingChanged = player.hasVehicle() != state.wasRiding;
        state.wasRiding = player.hasVehicle();
        if (moved || rotated || ridingChanged) {
            state.markActive(currentTick);
            if (state.isInLobby && ModConfig.get().returnOnActivity && state.returnCountdown == null) {
                state.returnCountdown = ModConfig.get().returnCountdownSeconds * 20;
            }
        }
        state.lastX = x; state.lastY = y; state.lastZ = z;
        state.lastYaw = yaw; state.lastPitch = pitch;
    }

    private void handleReturn(ServerPlayerEntity player, PlayerAfkState state, long currentTick) {
        if (state.returnCountdown == null) return;
        if (state.returnCountdown % 20 == 0 && state.returnCountdown > 0) {
            player.sendMessage(net.minecraft.text.Text.of("Returning in " + (state.returnCountdown / 20) + "..."), false);
        }
        state.returnCountdown--;
        if (state.returnCountdown <= 0) {
            lobby.returnPlayer(player, state);
        }
    }

    public PlayerAfkState getState(UUID uuid) { return states.get(uuid); }
    public Collection<PlayerAfkState> getAllStates() { return Collections.unmodifiableCollection(states.values()); }
    public void setState(UUID uuid, PlayerAfkState state) { states.put(uuid, state); }
}
