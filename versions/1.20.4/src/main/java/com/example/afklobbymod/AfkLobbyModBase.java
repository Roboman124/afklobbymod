package com.example.afklobbymod;

import com.example.afklobbymod.client.AfkLobbyClient;
import com.example.afklobbymod.config.AutoModConfig;
import com.example.afklobbymod.config.ModMenuIntegration;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfkLobbyModBase implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("afklobbymod");
    public static final String MOD_ID = "afklobbymod";

    private static AfkLobbyModBase instance;
    protected final ModConfig config = ModConfig.get();
    protected final LeaderboardStorage leaderboard = new LeaderboardStorage();
    protected final LobbyManager lobby = new LobbyManager();
    protected final AfkTracker tracker = new AfkTracker(lobby, leaderboard);
    protected final CeremonyScheduler ceremony = new CeremonyScheduler(lobby, leaderboard, tracker);

    @Override
    public void onInitialize() {
        instance = this;
        AutoConfig.register(AutoModConfig.class, GsonConfigSerializer::new);
        AfkLobbyClient.markClientOnly();

        LOGGER.info("AFK Lobby Mod {} initializing for {}", ModConfig.VERSION,
            FabricLoader.getInstance().getModContainer(MOD_ID).map(c -> c.getMetadata().getName()).orElse(MOD_ID));

        AfkCommands.register(lobby, tracker, leaderboard, ceremony);
        PlayerActivityEvents.register(tracker);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            tracker.onJoin(handler.player);
            leaderboard.load(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            tracker.onLeave(handler.player);
            leaderboard.save(server);
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            lobby.load(server);
            leaderboard.load(server);
            ceremony.load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            leaderboard.save(server);
            ceremony.save(server);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tracker.tick(server);
            ceremony.tick(server);
            if (server.getTicks() % config.hudSyncIntervalTicks == 0) {
                Networking.sendHudSync(server, leaderboard);
            }
        });
    }

    public static net.fabricmc.loader.api.ModContainer modContainer() { return FabricLoader.getInstance().getModContainer(MOD_ID).orElse(null); }
    public static java.nio.file.Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
}