package com.example.afklobbymod;

import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

public class PlayerActivityEvents {
    public static void register(AfkTracker tracker) {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player instanceof ServerPlayerEntity sp) tracker.onInteract(sp);
            return ActionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayerEntity sp) tracker.onInteract(sp);
            return ActionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayerEntity sp) tracker.onInteract(sp);
            return ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity sp) tracker.onUseItem(sp);
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> tracker.onChat(sender));
        ServerMessageEvents.COMMAND_MESSAGE.register((message, sender, params) -> {
            if (sender.getPlayer() != null) tracker.onCommand(sender.getPlayer());
        });
    }
}
