package com.example.afklobbymod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderboardStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, Long> storedMillis = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private Path dataPath;

    public void load(MinecraftServer server) {
        if (dataPath == null) dataPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("afklobbymod_player_time.json");
        if (!Files.exists(dataPath)) return;
        try {
            JsonObject json = GSON.fromJson(Files.readString(dataPath), JsonObject.class);
            if (json == null) return;
            Map<UUID, Long> times = GSON.fromJson(json.getAsJsonObject("times"), new TypeToken<Map<UUID, Long>>(){}.getType());
            Map<UUID, String> nameMap = GSON.fromJson(json.getAsJsonObject("names"), new TypeToken<Map<UUID, String>>(){}.getType());
            if (times != null) storedMillis.putAll(times);
            if (nameMap != null) names.putAll(nameMap);
        } catch (IOException e) {
            AfkLobbyMod.LOGGER.warn("Failed to load leaderboard", e);
        }
    }

    public void save(MinecraftServer server) {
        if (dataPath == null) dataPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("afklobbymod_player_time.json");
        try {
            JsonObject json = new JsonObject();
            json.add("times", GSON.toJsonTree(storedMillis));
            json.add("names", GSON.toJsonTree(names));
            Files.createDirectories(dataPath.getParent());
            Files.writeString(dataPath, GSON.toJson(json));
        } catch (IOException e) {
            AfkLobbyMod.LOGGER.warn("Failed to save leaderboard", e);
        }
    }

    public void addTime(UUID uuid, String name, long millis) {
        if (uuid == null || millis <= 0) return;
        storedMillis.merge(uuid, millis, Long::sum);
        if (name != null) names.put(uuid, name);
    }

    public void setTime(UUID uuid, String name, long millis) {
        storedMillis.put(uuid, millis);
        if (name != null) names.put(uuid, name);
    }

    public List<LeaderboardEntry> getTop(int n) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        storedMillis.forEach((uuid, millis) -> entries.add(new LeaderboardEntry(uuid, names.getOrDefault(uuid, uuid.toString()), millis)));
        entries.sort((a, b) -> Long.compare(b.millis, a.millis));
        return entries.subList(0, Math.min(n, entries.size()));
    }

    public LeaderboardEntry getWinner() {
        List<LeaderboardEntry> top = getTop(1);
        return top.isEmpty() ? null : top.get(0);
    }

    public long getTime(UUID uuid) {
        return storedMillis.getOrDefault(uuid, 0L);
    }

    public void clear() {
        storedMillis.clear();
        names.clear();
    }

    public Map<UUID, Long> getTimesSnapshot() {
        return new HashMap<>(storedMillis);
    }

    public static class LeaderboardEntry {
        public final UUID uuid;
        public final String name;
        public final long millis;
        public LeaderboardEntry(UUID uuid, String name, long millis) { this.uuid = uuid; this.name = name; this.millis = millis; }
    }
}
