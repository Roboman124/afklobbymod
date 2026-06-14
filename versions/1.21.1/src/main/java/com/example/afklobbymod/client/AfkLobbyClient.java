package com.example.afklobbymod.client;

import com.example.afklobbymod.Networking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AfkLobbyClient implements ClientModInitializer {
    private static boolean clientOnly = false;
    private KeyBinding toggleHudKey;

    public static void markClientOnly() {
        clientOnly = true;
    }

    @Override
    public void onInitializeClient() {
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.afklobbymod.toggle_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.afklobbymod.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleHudKey.wasPressed()) {
                boolean next = !HudRenderer.isEnabled();
                HudRenderer.setEnabled(next);
                ClientPlayNetworking.send(new Networking.ToggleHudPayload(next));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(Networking.HudSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                HudRenderer.setEnabled(payload.enabled());
                String[] names = payload.top().stream().map(e -> e.name).toArray(String[]::new);
                long[] times = payload.top().stream().mapToLong(e -> e.millis).toArray();
                HudRenderer.updateLeaderboard(names, times);
            });
        });

        HudRenderCallback.EVENT.register(new HudRenderInit());
    }
}
