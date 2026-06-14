package com.example.afklobbymod;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AfkTrackerTest {
    @Test
    public void markActiveResetsAfk() {
        PlayerAfkState state = new PlayerAfkState(UUID.randomUUID(), "Test");
        state.markAfk();
        assertTrue(state.isAfk);
        state.markActive(100);
        assertFalse(state.isAfk);
        assertEquals(100, state.lastActivityTick);
    }

    @Test
    public void totalAfkMillisAccumulates() throws InterruptedException {
        PlayerAfkState state = new PlayerAfkState(UUID.randomUUID(), "Test");
        state.markAfk();
        Thread.sleep(10);
        state.markActive(1);
        assertTrue(state.totalAfkMillis >= 5);
    }
}
