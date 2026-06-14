package com.example.afklobbymod.client;

import com.example.afklobbymod.Networking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
                net.minecraft.network.PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(next);
                ClientPlayNetworking.send(Networking.TOGGLE_HUD, buf);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(Networking.HUD_SYNC, (client, handler, buf, responseSender) -> {
            boolean enabled = buf.readBoolean();
            int size = buf.readInt();
            String[] names = new String[size];
            long[] times = new long[size];
            for (int i = 0; i < size; i++) {
                buf.readUuid();
                names[i] = buf.readString();
                times[i] = buf.readLong();
            }
            client.execute(() -> {
                HudRenderer.setEnabled(enabled);
                HudRenderer.updateLeaderboard(names, times);
            });
        });

        HudRenderCallback.EVENT.register(new HudRenderInit());
    }
}
