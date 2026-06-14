package com.example.afklobbymod;

import com.example.afklobbymod.integration.LuckPermsIntegration;
import com.google.gson.JsonObject;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CeremonyScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("afklobbymod");
    private final LobbyManager lobby;
    private final LeaderboardStorage leaderboard;
    private final AfkTracker tracker;
    private final Path ceremonyConfigPath = AfkLobbyModBase.configDir().resolve("afklobbymod_ceremony.json");
    private final Random random = new Random();
    private final List<UUID> participants = new ArrayList<>();

    private boolean ranToday = false;
    private boolean ceremonyActive = false;
    private int ceremonyTick = 0;
    private UUID previousWinner;
    private UUID currentWinner;

    public CeremonyScheduler(LobbyManager lobby, LeaderboardStorage leaderboard, AfkTracker tracker) {
        this.lobby = lobby;
        this.leaderboard = leaderboard;
        this.tracker = tracker;
    }

    public void load(MinecraftServer server) {
        if (!Files.exists(ceremonyConfigPath)) return;
        try {
            JsonObject json = ModConfig.GSON.fromJson(Files.readString(ceremonyConfigPath), JsonObject.class);
            if (json == null) return;
            if (json.has("ranToday")) ranToday = json.get("ranToday").getAsBoolean();
            if (json.has("previousWinner")) previousWinner = UUID.fromString(json.get("previousWinner").getAsString());
            if (json.has("currentWinner")) currentWinner = UUID.fromString(json.get("currentWinner").getAsString());
        } catch (IOException e) {
            AfkLobbyMod.LOGGER.warn("Failed to load ceremony state", e);
        }
    }

    public void save(MinecraftServer server) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("ranToday", ranToday);
            if (previousWinner != null) json.addProperty("previousWinner", previousWinner.toString());
            if (currentWinner != null) json.addProperty("currentWinner", currentWinner.toString());
            Files.createDirectories(ceremonyConfigPath.getParent());
            Files.writeString(ceremonyConfigPath, ModConfig.GSON.toJson(json));
        } catch (IOException e) {
            AfkLobbyMod.LOGGER.warn("Failed to save ceremony state", e);
        }
    }

    public void tick(MinecraftServer server) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enableCeremony) return;
        if (!ceremonyActive) {
            checkDailyTrigger(server);
            return;
        }
        runCeremonyTick(server);
    }

    private void checkDailyTrigger(MinecraftServer server) {
        ModConfig cfg = ModConfig.get();
        ZoneId zone = ZoneId.of(cfg.ceremonyTimezone);
        LocalTime now = LocalTime.now(zone);
        LocalTime target;
        try {
            target = LocalTime.parse(cfg.ceremonyTime, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            target = LocalTime.of(20, 0);
        }
        if (now.isAfter(target) || now.equals(target)) {
            if (!ranToday) {
                startCeremony(server);
            }
        } else {
            ranToday = false;
        }
    }

    public void startCeremonyNow(MinecraftServer server) {
        ranToday = false;
        startCeremony(server);
    }

    private void startCeremony(MinecraftServer server) {
        ceremonyActive = true;
        ceremonyTick = 0;
        ranToday = true;
        participants.clear();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            participants.add(p.getUuid());
        }

        LeaderboardStorage.LeaderboardEntry winner = leaderboard.getWinner();
        if (winner == null) {
            ceremonyActive = false;
            return;
        }
        currentWinner = winner.uuid;

        broadcast(server, "The daily AFK ceremony is beginning!");

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            lobby.teleportToStage(p);
            if (ModConfig.get().spectatorDuringCeremony) {
                p.changeGameMode(GameMode.SPECTATOR);
            }
        }
        save(server);
    }

    private void runCeremonyTick(MinecraftServer server) {
        ModConfig cfg = ModConfig.get();
        ServerWorld world = lobby.resolveWorld(lobby.getStageWorldId());
        if (world == null) world = server.getOverworld();
        Vec3d stage = lobby.getStagePos();

        // Phase 0-3s: suspense ticking / wall break sounds
        if (ceremonyTick < 60) {
            if (ceremonyTick % 10 == 0) {
                playSound(world, stage, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 0.5f + (ceremonyTick / 120f));
            }
            if (cfg.animatedWall && ceremonyTick % 5 == 0) {
                spawnWallParticles(world, stage);
            }
        }
        // Phase 3-6s: countdown
        else if (ceremonyTick < 120) {
            int countdown = 3 - (ceremonyTick - 60) / 20;
            if ((ceremonyTick - 60) % 20 == 0 && countdown > 0) {
                broadcast(server, "Winner announcement in " + countdown);
                playSound(world, stage, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1f, 1f);
            }
        }
        // Phase 6-10s: announce
        else if (ceremonyTick < 200) {
            if (ceremonyTick == 120) announceWinner(server);
        }
        // Phase 10-15s: fireworks + particles
        else if (ceremonyTick < 300) {
            if (ceremonyTick % 10 == 0) {
                spawnFireworks(world, stage, 5 + random.nextInt(5));
                spawnConfetti(world, stage);
            }
        }
        // Phase 15s+: end
        else {
            endCeremony(server);
        }
        ceremonyTick++;
    }

    private void announceWinner(MinecraftServer server) {
        if (currentWinner == null) return;
        ServerPlayerEntity winnerPlayer = server.getPlayerManager().getPlayer(currentWinner);
        String name = winnerPlayer != null ? winnerPlayer.getName().getString() : leaderboard.getWinner().name;
        Text title = Text.literal(name).formatted(Formatting.GOLD, Formatting.BOLD);
        Text subtitle = Text.literal("is today's AFK champion!").formatted(Formatting.YELLOW);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(Text.of("Today's AFK champion is "), false);
            p.sendMessage(title, false);
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
            p.networkHandler.sendPacket(new TitleS2CPacket(title));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        }
        if (winnerPlayer != null) {
            spawnCrown(winnerPlayer);
            if (ModConfig.get().enableLuckPermsPrefix) {
                try {
                    LuckPermsIntegration.applyPrefix(currentWinner, ModConfig.get().winnerPrefix, ModConfig.get().winnerPrefixColor);
                    if (previousWinner != null && !previousWinner.equals(currentWinner)) {
                        LuckPermsIntegration.removePrefix(previousWinner, ModConfig.get().winnerPrefix);
                    }
                } catch (Exception e) {
                    LOGGER.warn("LuckPerms prefix update failed (is LuckPerms installed/loaded?); continuing ceremony", e);
                }
            }
        }
        previousWinner = currentWinner;
    }

    private void endCeremony(MinecraftServer server) {
        ceremonyActive = false;
        ceremonyTick = 0;
        broadcast(server, "Ceremony complete! Returning everyone to their original positions.");
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            PlayerAfkState state = tracker.getState(p.getUuid());
            if (state != null && state.isInLobby) {
                lobby.returnPlayer(p, state);
            }
            if (ModConfig.get().spectatorDuringCeremony) {
                p.changeGameMode(GameMode.SURVIVAL);
            }
        }
        participants.clear();
        save(server);
    }

    private void spawnCrown(ServerPlayerEntity player) {
        ItemStack crown = new ItemStack(Items.GOLDEN_HELMET);
        NbtCompound tag = new NbtCompound();
        tag.putString("afklobbymod.crown", "true");
        tag.putInt("Unbreakable", 1);
        crown.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        player.getInventory().setStack(39, crown);
    }

    private void playSound(ServerWorld world, Vec3d pos, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.MASTER, volume, pitch);
    }

    private void spawnFireworks(ServerWorld world, Vec3d pos, int count) {
        for (int i = 0; i < count; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            NbtCompound tag = new NbtCompound();
            NbtCompound firework = new NbtCompound();
            firework.putByte("Flight", (byte) 1);
            tag.put("Fireworks", firework);
            rocket.set(DataComponentTypes.FIREWORKS, new FireworksComponent((byte) 1, java.util.List.of()));
            double dx = (world.random.nextDouble() - 0.5) * 10;
            double dz = (world.random.nextDouble() - 0.5) * 10;
            EntityType.FIREWORK_ROCKET.spawn(world, BlockPos.ofFloored(pos.x + dx, pos.y, pos.z + dz), SpawnReason.EVENT);
        }
    }

    private void spawnConfetti(ServerWorld world, Vec3d pos) {
        for (int i = 0; i < 30; i++) {
            double dx = (world.random.nextDouble() - 0.5) * 8;
            double dy = world.random.nextDouble() * 4;
            double dz = (world.random.nextDouble() - 0.5) * 8;
            world.spawnParticles(net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER, pos.x + dx, pos.y + dy, pos.z + dz, 1, 0, 0.5, 0, 0.5);
        }
    }

    private void spawnWallParticles(ServerWorld world, Vec3d pos) {
        for (int i = 0; i < 20; i++) {
            double x = pos.x + (world.random.nextDouble() - 0.5) * 12;
            double y = pos.y + world.random.nextDouble() * 6;
            double z = pos.z - 6 + world.random.nextDouble() * 12;
            world.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT, x, y, z, 1, 0, 0, 0, 0.2);
        }
    }

    private void broadcast(MinecraftServer server, String msg) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(Text.of(msg), false);
        }
    }

    public boolean isCeremonyActive() { return ceremonyActive; }
}
