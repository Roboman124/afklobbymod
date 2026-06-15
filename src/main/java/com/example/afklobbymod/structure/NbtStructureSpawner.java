package com.example.afklobbymod.structure;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Spawns structures authored in external NBT files.
 *
 * The two bundled structures are:
 *  - {@code afklobbymod:lobby} (the Celestial Gazebo replacement) - placed
 *    high in the air.
 *  - {@code afklobbymod:stage} (the Town Square Theater replacement) - placed
 *    on the ground at the player's feet.
 */
public final class NbtStructureSpawner {

    private static final String LOBBY_ID = "afklobbymod:lobby";
    private static final String STAGE_ID = "afklobbymod:stage";

    /** How high above the command origin the lobby should float. */
    private static final int LOBBY_AIR_OFFSET = 80;

    private NbtStructureSpawner() {
        // Utility class
    }

    /**
     * Spawns the bundled lobby structure high in the air, centered on the
     * supplied {@code origin} horizontally.
     */
    public static void spawnLobby(ServerWorld world, BlockPos origin) {
        BlockPos spawnPos = origin.up(LOBBY_AIR_OFFSET);
        spawn(world, LOBBY_ID, spawnPos);
    }

    /**
     * Spawns the bundled stage structure on the ground at the supplied
     * {@code origin}.
     */
    public static void spawnStage(ServerWorld world, BlockPos origin) {
        spawn(world, STAGE_ID, origin);
    }

    private static void spawn(ServerWorld world, String id, BlockPos origin) {
        StructureTemplate template = loadTemplate(world, id);
        if (template == null) {
            com.example.afklobbymod.AfkLobbyMod.LOGGER.warn("Could not load structure '{}'", id);
            return;
        }

        StructurePlacementData placement = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(BlockRotation.NONE)
                .setIgnoreEntities(false);

        Vec3i size = template.getSize();
        BlockPos corner = origin.add(-(size.getX() / 2), 0, -(size.getZ() / 2));

        template.place(world, corner, corner, placement, Random.create(), Block.NOTIFY_ALL);
    }

    private static StructureTemplate loadTemplate(ServerWorld world, String id) {
        StructureTemplateManager manager = world.getStructureTemplateManager();
        Identifier identifier = Identifier.tryParse(id);

        // Modern path: the manager can load NBT from data/<namespace>/structures/.
        Optional<StructureTemplate> opt = manager.getTemplate(identifier);
        if (opt.isPresent()) {
            return opt.get();
        }

        // Fallback 1: read the bundled NBT file via the Fabric mod container.
        String resourcePath = "data/" + id.replace(':', '/') + ".nbt";
        Optional<Path> pathOpt = findBundledPath(resourcePath);
        if (pathOpt.isPresent()) {
            try (InputStream stream = Files.newInputStream(pathOpt.get())) {
                NbtCompound nbt = readCompressed(stream);
                return manager.createTemplate(nbt);
            } catch (IOException e) {
                com.example.afklobbymod.AfkLobbyMod.LOGGER.warn("Failed to read structure '{}' from resource path", id, e);
            }
        }

        // Fallback 2: try Class.getResourceAsStream in case the mod is loaded in a
        // development / flat classpath environment where the mod container path is empty.
        try (InputStream stream = NbtStructureSpawner.class.getResourceAsStream("/" + resourcePath)) {
            if (stream != null) {
                NbtCompound nbt = readCompressed(stream);
                return manager.createTemplate(nbt);
            }
        } catch (IOException e) {
            com.example.afklobbymod.AfkLobbyMod.LOGGER.warn("Failed to read structure '{}' via classpath", id, e);
        }

        com.example.afklobbymod.AfkLobbyMod.LOGGER.warn("Missing bundled structure resource: {}", resourcePath);
        return null;
    }

    /**
     * Tries to locate a file inside the mod jar using several Fabric loader APIs.
     */
    private static Optional<Path> findBundledPath(String resourcePath) {
        Optional<ModContainer> containerOpt = FabricLoader.getInstance()
                .getModContainer(com.example.afklobbymod.AfkLobbyMod.MOD_ID);
        if (containerOpt.isEmpty()) {
            return Optional.empty();
        }

        ModContainer container = containerOpt.get();

        // Method 1: direct findPath (works for most Fabric loader versions).
        Optional<Path> direct = container.findPath(resourcePath);
        if (direct.isPresent()) {
            return direct;
        }

        // Method 2: search through all root paths in case findPath is restricted.
        for (Path root : container.getRootPaths()) {
            Path candidate = root.resolve(resourcePath);
            if (Files.exists(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    /**
     * Reads a compressed NBT compound from an input stream in a way that is
     * compatible with both the old {@code NbtTagSizeTracker} and the newer
     * {@code NbtSizeTracker} class names.
     */
    private static NbtCompound readCompressed(InputStream stream) throws IOException {
        String[] trackerClassNames = {
                "net.minecraft.nbt.NbtSizeTracker",
                "net.minecraft.nbt.NbtTagSizeTracker"
        };

        for (String className : trackerClassNames) {
            try {
                Class<?> trackerClass = Class.forName(className);
                Method ofUnlimited = trackerClass.getMethod("ofUnlimitedBytes");
                Object tracker = ofUnlimited.invoke(null);
                Method readMethod = NbtIo.class.getMethod("readCompressed", InputStream.class, trackerClass);
                return (NbtCompound) readMethod.invoke(null, stream, tracker);
            } catch (Exception ignored) {
                // Try the other class name.
            }
        }

        throw new IOException("Unable to read compressed NBT with any known size tracker class");
    }
}
