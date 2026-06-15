package com.example.afklobbymod.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import com.example.afklobbymod.AfkLobbyMod;

@Config(name = AfkLobbyMod.MOD_ID)
public class AutoModConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public int afkTimeoutSeconds = 300;
    @ConfigEntry.Gui.Tooltip
    public int afkWarningSeconds = 10;
    @ConfigEntry.Gui.Tooltip
    public int returnCountdownSeconds = 3;
    @ConfigEntry.Gui.Tooltip
    public boolean teleportToLobby = true;
    @ConfigEntry.Gui.Tooltip
    public boolean returnOnActivity = true;
    @ConfigEntry.Gui.Tooltip
    public boolean persistTimeTracking = true;
    @ConfigEntry.Gui.Tooltip
    public boolean enableCeremony = true;
    public String ceremonyTime = "20:00";
    public String ceremonyTimezone = "UTC";
    public int ceremonyDurationSeconds = 60;
    @ConfigEntry.Gui.Tooltip
    public boolean enableLeaderboardHud = true;
    public int hudSyncIntervalTicks = 20;
    public int leaderboardSize = 10;
    @ConfigEntry.Gui.Tooltip
    public boolean enableLuckPermsPrefix = true;
    public String winnerPrefix = "AFK King";
    public String winnerPrefixColor = "#FFD700";
    public boolean enableParticles = true;
    public boolean enableCustomSounds = true;
    public boolean animatedWall = true;
    public boolean spectatorDuringCeremony = false;
    public boolean debug = false;
}
