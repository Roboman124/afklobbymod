package com.example.afklobbymod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.concurrent.TimeUnit;

public class AfkCommands {
    public static void register(LobbyManager lobby, AfkTracker tracker, LeaderboardStorage leaderboard, CeremonyScheduler ceremony) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerAll(dispatcher, lobby, tracker, leaderboard, ceremony));
    }

    private static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher, LobbyManager lobby, AfkTracker tracker, LeaderboardStorage leaderboard, CeremonyScheduler ceremony) {
        dispatcher.register(CommandManager.literal("afktracker")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("getstarted")
                .executes(ctx -> {
                    getStarted(ctx.getSource(), lobby);
                    return 1;
                }))
            .then(CommandManager.literal("setlobby")
                .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
                    .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
                        .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                if (p == null) { reply(ctx.getSource(), "Run as a player."); return 0; }
                                lobby.setLobby((net.minecraft.server.world.ServerWorld) p.getWorld(), DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "y"), DoubleArgumentType.getDouble(ctx, "z"));
                                reply(ctx.getSource(), "Lobby set.");
                                return 1;
                            })))))
            .then(CommandManager.literal("setstage")
                .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
                    .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
                        .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                if (p == null) { reply(ctx.getSource(), "Run as a player."); return 0; }
                                lobby.setStage((net.minecraft.server.world.ServerWorld) p.getWorld(), DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "y"), DoubleArgumentType.getDouble(ctx, "z"));
                                reply(ctx.getSource(), "Stage set.");
                                return 1;
                            })))))
            .then(CommandManager.literal("leaderboard")
                .executes(ctx -> {
                    showLeaderboard(ctx.getSource(), leaderboard);
                    return 1;
                }))
            .then(CommandManager.literal("time")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> {
                        ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "player");
                        long ms = leaderboard.getTime(p.getUuid());
                        reply(ctx.getSource(), p.getName().getString() + ": " + formatTime(ms));
                        return 1;
                    })))
            .then(CommandManager.literal("reload")
                .executes(ctx -> {
                    ModConfig.reload();
                    reply(ctx.getSource(), "Config reloaded.");
                    return 1;
                }))
            .then(CommandManager.literal("startceremony")
                .executes(ctx -> {
                    ceremony.startCeremonyNow(ctx.getSource().getServer());
                    reply(ctx.getSource(), "Ceremony started.");
                    return 1;
                }))
            .then(CommandManager.literal("hud")
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (p == null) { reply(ctx.getSource(), "Player-only command."); return 0; }
                        Networking.setHudEnabled(p, BoolArgumentType.getBool(ctx, "enabled"));
                        reply(ctx.getSource(), "HUD " + (BoolArgumentType.getBool(ctx, "enabled") ? "enabled" : "disabled") + ".");
                        return 1;
                    })))
        );
    }

    private static void getStarted(ServerCommandSource source, LobbyManager lobby) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            reply(source, "Run as a player to auto-set lobby at your feet and stage 10 blocks in front of you.");
            return;
        }
        lobby.setLobby((net.minecraft.server.world.ServerWorld) player.getWorld(), player.getX(), player.getY(), player.getZ());
        lobby.setStage((net.minecraft.server.world.ServerWorld) player.getWorld(), player.getX() + player.getRotationVector().x * 10, player.getY(), player.getZ() + player.getRotationVector().z * 10);
        reply(source, "Lobby and stage auto-configured. Edit config/afklobbymod.json to tune times.");
    }

    private static void showLeaderboard(ServerCommandSource source, LeaderboardStorage leaderboard) {
        var top = leaderboard.getTop(10);
        source.sendMessage(Text.of("=== AFK Leaderboard ==="));
        int i = 1;
        for (var e : top) {
            source.sendMessage(Text.of(i + ". " + e.name + " - " + formatTime(e.millis)));
            i++;
        }
    }

    private static String formatTime(long millis) {
        long h = TimeUnit.MILLISECONDS.toHours(millis);
        long m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static void reply(ServerCommandSource source, String msg) {
        source.sendMessage(Text.of(msg));
    }
}
