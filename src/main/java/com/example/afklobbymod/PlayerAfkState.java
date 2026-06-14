package com.example.afklobbymod;

import java.util.UUID;

public class PlayerAfkState {
    public final UUID uuid;
    public String name;

    public double lastX, lastY, lastZ;
    public float lastYaw, lastPitch;
    public long lastActivityTick = -1;
    public boolean isAfk = false;
    public boolean isInLobby = false;

    public Integer afkCountdown = null;
    public Integer returnCountdown = null;

    public String originalWorldId;
    public double originalX, originalY, originalZ;
    public float originalYaw, originalPitch;

    public boolean wasRiding = false;
    public long totalAfkMillis = 0;
    public long currentAfkStartMillis = 0;

    public PlayerAfkState(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public void markActive(long tick) {
        if (isAfk && currentAfkStartMillis != 0) {
            totalAfkMillis += System.currentTimeMillis() - currentAfkStartMillis;
        }
        isAfk = false;
        currentAfkStartMillis = 0;
        lastActivityTick = tick;
        afkCountdown = null;
    }

    public void markAfk() {
        if (!isAfk) {
            isAfk = true;
            currentAfkStartMillis = System.currentTimeMillis();
        }
    }

    public long getCurrentAfkMillis() {
        if (isAfk && currentAfkStartMillis != 0) {
            return totalAfkMillis + (System.currentTimeMillis() - currentAfkStartMillis);
        }
        return totalAfkMillis;
    }

    public void resetReturn() {
        returnCountdown = null;
        isInLobby = false;
        originalWorldId = null;
    }
}
