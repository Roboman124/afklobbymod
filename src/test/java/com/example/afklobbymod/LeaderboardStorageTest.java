package com.example.afklobbymod;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LeaderboardStorageTest {
    @Test
    public void addTimeAndGetTop() {
        LeaderboardStorage lb = new LeaderboardStorage();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        lb.addTime(a, "Alice", 1000);
        lb.addTime(b, "Bob", 2000);
        lb.addTime(a, "Alice", 500);
        var top = lb.getTop(2);
        assertEquals(2, top.size());
        assertEquals("Bob", top.get(0).name);
        assertEquals(2000, top.get(0).millis);
        assertEquals("Alice", top.get(1).name);
        assertEquals(1500, top.get(1).millis);
    }

    @Test
    public void getWinner() {
        LeaderboardStorage lb = new LeaderboardStorage();
        UUID id = UUID.randomUUID();
        lb.setTime(id, "Champion", 9999);
        var winner = lb.getWinner();
        assertNotNull(winner);
        assertEquals("Champion", winner.name);
    }

    @Test
    public void clear() {
        LeaderboardStorage lb = new LeaderboardStorage();
        lb.setTime(UUID.randomUUID(), "X", 100);
        lb.clear();
        assertTrue(lb.getTop(10).isEmpty());
    }
}
