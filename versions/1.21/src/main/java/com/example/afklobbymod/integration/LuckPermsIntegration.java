package com.example.afklobbymod.integration;

import com.example.afklobbymod.AfkLobbyMod;
import com.example.afklobbymod.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;

import java.util.UUID;

public class LuckPermsIntegration {
    private static boolean checked = false;
    private static LuckPerms api = null;

    private static LuckPerms getApi() {
        if (!checked) {
            checked = true;
            if (FabricLoader.getInstance().isModLoaded("luckperms")) {
                api = net.fabricmc.loader.api.FabricLoader.getInstance().getObjectShare().get("luckperms:api") instanceof LuckPerms l ? l : null;
                if (api == null) {
                    try {
                        api = net.luckperms.api.LuckPermsProvider.get();
                    } catch (Exception ignored) {}
                }
            }
        }
        return api;
    }

    public static void applyPrefix(UUID uuid, String prefix, String color) {
        LuckPerms lp = getApi();
        if (lp == null || !ModConfig.get().enableLuckPermsPrefix) return;
        lp.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;
            removeExistingAfkPrefix(user);
            user.data().add(PrefixNode.builder(color + "[" + prefix + "] ", 100).build());
            lp.getUserManager().saveUser(user);
        });
    }

    public static void removePrefix(UUID uuid, String prefix) {
        LuckPerms lp = getApi();
        if (lp == null) return;
        lp.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;
            removeExistingAfkPrefix(user);
            lp.getUserManager().saveUser(user);
        });
    }

    private static void removeExistingAfkPrefix(User user) {
        user.data().clear(node -> {
            if (!node.getType().equals(NodeType.PREFIX)) return false;
            String prefix = ((PrefixNode) node).getMetaValue();
            return prefix.contains("[" + ModConfig.get().winnerPrefix + "]");
        });
    }
}
