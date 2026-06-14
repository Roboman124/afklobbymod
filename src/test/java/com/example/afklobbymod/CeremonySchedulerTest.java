package com.example.afklobbymod;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class CeremonySchedulerTest {
    @Test
    public void parsesCeremonyTime() {
        LocalTime time = LocalTime.parse("20:00", DateTimeFormatter.ofPattern("HH:mm"));
        assertEquals(20, time.getHour());
        assertEquals(0, time.getMinute());
    }

    @Test
    public void emptyLeaderboardHasNoWinner() {
        LeaderboardStorage lb = new LeaderboardStorage();
        assertNull(lb.getWinner());
    }
}
