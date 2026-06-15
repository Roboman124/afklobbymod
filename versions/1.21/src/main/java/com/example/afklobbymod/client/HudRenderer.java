package com.example.afklobbymod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.concurrent.TimeUnit;

public class HudRenderer {
    private static boolean enabled = true;
    private static String[] names = new String[0];
    private static long[] times = new long[0];

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean v) { enabled = v; }

    public static void updateLeaderboard(String[] n, long[] t) {
        names = n; times = t;
    }

    public static void render(DrawContext context) {
        if (!enabled || names == null || names.length == 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) return;
        int x = 10, y = 10;
        context.drawTextWithShadow(client.textRenderer, Text.literal("AFK Leaderboard").styled(s -> s.withBold(true)), x, y, 0xFFD700);
        y += 12;
        for (int i = 0; i < names.length; i++) {
            String line = (i + 1) + ". " + names[i] + " " + format(times[i]);
            context.drawTextWithShadow(client.textRenderer, Text.literal(line), x, y, 0xFFFFFF);
            y += 10;
        }
    }

    private static String format(long millis) {
        long h = TimeUnit.MILLISECONDS.toHours(millis);
        long m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return String.format("%02d:%02d", h, m);
    }
}
