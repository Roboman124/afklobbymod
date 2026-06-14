package com.example.afklobbymod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    public static final String VERSION = "2.0.0";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("afklobbymod.json");

    public int afkTimeoutSeconds = 300;
    public int afkWarningSeconds = 10;
    public int returnCountdownSeconds = 3;
    public boolean teleportToLobby = true;
    public boolean returnOnActivity = true;
    public boolean persistTimeTracking = true;
    public boolean enableCeremony = true;
    public String ceremonyTime = "20:00";
    public String ceremonyTimezone = "UTC";
    public int ceremonyDurationSeconds = 60;
    public boolean enableLeaderboardHud = true;
    public int hudSyncIntervalTicks = 20;
    public int leaderboardSize = 10;
    public boolean enableLuckPermsPrefix = true;
    public String winnerPrefix = "AFK King";
    public String winnerPrefixColor = "#FFD700";
    public boolean enableParticles = true;
    public boolean enableCustomSounds = true;
    public boolean animatedWall = true;
    public boolean spectatorDuringCeremony = false;
    public boolean debug = false;

    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null) {
            instance = new ModConfig();
            instance.load();
        }
        return instance;
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) { save(); return; }
        try {
            JsonObject json = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
            if (json == null) return;
            if (json.has("afkTimeoutSeconds")) afkTimeoutSeconds = json.get("afkTimeoutSeconds").getAsInt();
            if (json.has("afkWarningSeconds")) afkWarningSeconds = json.get("afkWarningSeconds").getAsInt();
            if (json.has("returnCountdownSeconds")) returnCountdownSeconds = json.get("returnCountdownSeconds").getAsInt();
            if (json.has("teleportToLobby")) teleportToLobby = json.get("teleportToLobby").getAsBoolean();
            if (json.has("returnOnActivity")) returnOnActivity = json.get("returnOnActivity").getAsBoolean();
            if (json.has("persistTimeTracking")) persistTimeTracking = json.get("persistTimeTracking").getAsBoolean();
            if (json.has("enableCeremony")) enableCeremony = json.get("enableCeremony").getAsBoolean();
            if (json.has("ceremonyTime")) ceremonyTime = json.get("ceremonyTime").getAsString();
            if (json.has("ceremonyTimezone")) ceremonyTimezone = json.get("ceremonyTimezone").getAsString();
            if (json.has("ceremonyDurationSeconds")) ceremonyDurationSeconds = json.get("ceremonyDurationSeconds").getAsInt();
            if (json.has("enableLeaderboardHud")) enableLeaderboardHud = json.get("enableLeaderboardHud").getAsBoolean();
            if (json.has("hudSyncIntervalTicks")) hudSyncIntervalTicks = json.get("hudSyncIntervalTicks").getAsInt();
            if (json.has("leaderboardSize")) leaderboardSize = json.get("leaderboardSize").getAsInt();
            if (json.has("enableLuckPermsPrefix")) enableLuckPermsPrefix = json.get("enableLuckPermsPrefix").getAsBoolean();
            if (json.has("winnerPrefix")) winnerPrefix = json.get("winnerPrefix").getAsString();
            if (json.has("winnerPrefixColor")) winnerPrefixColor = json.get("winnerPrefixColor").getAsString();
            if (json.has("enableParticles")) enableParticles = json.get("enableParticles").getAsBoolean();
            if (json.has("enableCustomSounds")) enableCustomSounds = json.get("enableCustomSounds").getAsBoolean();
            if (json.has("animatedWall")) animatedWall = json.get("animatedWall").getAsBoolean();
            if (json.has("spectatorDuringCeremony")) spectatorDuringCeremony = json.get("spectatorDuringCeremony").getAsBoolean();
            if (json.has("debug")) debug = json.get("debug").getAsBoolean();
        } catch (IOException e) {
            AfkLobbyMod.LOGGER.warn("Failed to load config", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            AfkLobbyMod.LOGGER.warn("Failed to save config", e);
        }
    }

    public static void reload() {
        instance = null;
        get();
    }
}
