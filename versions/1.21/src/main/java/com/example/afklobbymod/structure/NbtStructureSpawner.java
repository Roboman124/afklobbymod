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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

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
        // The files live under data/<namespace>/structures/<name>.nbt.
        String structureName = id.substring(id.indexOf(':') + 1);
        String resourcePath = "data/afklobbymod/structures/" + structureName + ".nbt";
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
     * Reads a Minecraft compressed NBT structure file. The file is gzip-compressed
     * and contains a single root NbtCompound.
     *
     * We locate the appropriate {@code NbtIo} method by signature rather than by
     * Yarn name so this works under Fabric's runtime mappings in every version
     * (e.g. 1.19.4 calls it {@code read(DataInput)}, newer versions call it
     * {@code readCompound(DataInput)}).
     */
    private static NbtCompound readCompressed(InputStream stream) throws IOException {
        // Structure NBT files are gzip-compressed.
        try (DataInputStream data = new DataInputStream(new GZIPInputStream(stream))) {
            for (Method method : NbtIo.class.getDeclaredMethods()) {
                if (method.getReturnType() != NbtCompound.class) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || params[0] != java.io.DataInput.class) continue;
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
                try {
                    method.setAccessible(true);
                    return (NbtCompound) method.invoke(null, data);
                } catch (ReflectiveOperationException e) {
                    throw new IOException("Failed to invoke NbtIo reader", e);
                }
            }
        }
        throw new IOException("Unable to find a suitable NbtIo.readCompound(DataInput) method");
    }
}
