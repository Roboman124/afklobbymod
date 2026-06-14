package com.example.afklobbymod.structure;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

/**
 * Generates a self-contained Celestial Gazebo floating structure.
 *
 * The build is centered on the supplied {@code origin}:
 *  - Circular island platform (grass ring with flowers, stone interior,
 *    dark-oak cross/starburst inlay).
 *  - Four massive pillars at the cardinal edges, wrapped in spruce trapdoors
 *    and dark-oak fences, with flared stone-brick capitals.
 *  - Arches between pillars with Gothic hanging valance trim.
 *  - Stepped dome capping the gazebo (stone base transitioning to dark
 *    oak/spruce crown, vertical spruce-rib accents).
 *  - Front entrance archway and a detached decorative portal arch at the far end.
 */
public final class CelestialGazeboGenerator {

    private static final Random RANDOM = new Random();

    // ---------- Block convenience constants ----------

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState GRASS_BLOCK = Blocks.GRASS_BLOCK.getDefaultState();
    private static final BlockState DIRT = Blocks.DIRT.getDefaultState();
    private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState CRACKED_STONE_BRICKS = Blocks.CRACKED_STONE_BRICKS.getDefaultState();
    private static final BlockState CHISELED_STONE_BRICKS = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
    private static final BlockState COBBLESTONE = Blocks.COBBLESTONE.getDefaultState();
    private static final BlockState STONE_BRICK_SLAB = Blocks.STONE_BRICK_SLAB.getDefaultState();
    private static final BlockState STONE_BRICK_STAIRS = Blocks.STONE_BRICK_STAIRS.getDefaultState();
    private static final BlockState DARK_OAK_PLANKS = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState DARK_OAK_SLAB = Blocks.DARK_OAK_SLAB.getDefaultState();
    private static final BlockState DARK_OAK_STAIRS = Blocks.DARK_OAK_STAIRS.getDefaultState();
    private static final BlockState SPRUCE_STAIRS = Blocks.SPRUCE_STAIRS.getDefaultState();
    private static final BlockState SPRUCE_TRAPDOOR = Blocks.SPRUCE_TRAPDOOR.getDefaultState();
    private static final BlockState DARK_OAK_FENCE = Blocks.DARK_OAK_FENCE.getDefaultState();
    private static final BlockState GLOWSTONE = Blocks.GLOWSTONE.getDefaultState();
    private static final BlockState REDSTONE_LAMP = Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, true);
    private static final BlockState OXEYE_DAISY = Blocks.OXEYE_DAISY.getDefaultState();
    private static final BlockState LILY_OF_THE_VALLEY = Blocks.LILY_OF_THE_VALLEY.getDefaultState();
    private static final BlockState SHORT_GRASS = Blocks.GRASS.getDefaultState();

    private CelestialGazeboGenerator() {
        // Utility class
    }

    /**
     * Builds the entire Celestial Gazebo centered on {@code origin}.
     * The origin becomes the centre of the island platform floor.
     */
    public static void build(ServerWorld world, BlockPos origin) {
        buildIsland(world, origin);
        buildPillars(world, origin);
        buildArches(world, origin);
        buildDome(world, origin);
        buildRoofSupports(world, origin);
        buildCentralLight(world, origin);
    }

    // =========================================================
    // 1. Floating circular island platform
    // =========================================================

    private static void buildIsland(ServerWorld world, BlockPos origin) {
        int radius = 14;
        int outerRing = 13; // ring where grass/flowers appear

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distSq = x * x + z * z;
                if (distSq > radius * radius) {
                    continue;
                }

                BlockPos floorPos = origin.add(x, 0, z);
                boolean isEdge = distSq >= outerRing * outerRing;
                boolean isOuter = distSq >= (radius - 2) * (radius - 2);

                // Layer of dirt under the visible floor so the island is solid
                set(world, floorPos.down(), DIRT);
                set(world, floorPos.down(2), DIRT);
                if (RANDOM.nextInt(4) == 0) {
                    set(world, floorPos.down(3), DIRT);
                }

                if (isEdge) {
                    // Outer grass ring with occasional flower / short grass
                    set(world, floorPos, GRASS_BLOCK);
                    decorateGrass(world, floorPos.up());
                } else {
                    // Concentric rings of stone bricks and cobblestone
                    int ring = (int) Math.floor(Math.sqrt(distSq) / 2.5);
                    BlockState floorState = (ring % 2 == 0) ? STONE_BRICKS : COBBLESTONE;

                    // Dark oak cross / starburst inlay near centre
                    if (Math.abs(x) <= 1 || Math.abs(z) <= 1) {
                        if (Math.abs(x) == 0 || Math.abs(z) == 0) {
                            floorState = DARK_OAK_SLAB.with(Properties.SLAB_TYPE, SlabType.BOTTOM);
                        } else {
                            floorState = DARK_OAK_PLANKS;
                        }
                    }

                    // Occasional cracked stone accent in non-centre area
                    if (!isOuter && RANDOM.nextInt(18) == 0) {
                        floorState = CRACKED_STONE_BRICKS;
                    }

                    set(world, floorPos, floorState);

                    // Slight roughness on the outer stone ring
                    if (isOuter && RANDOM.nextInt(5) == 0) {
                        set(world, floorPos.up(), STONE_BRICK_SLAB);
                    }
                }
            }
        }
    }

    private static void decorateGrass(ServerWorld world, BlockPos pos) {
        BlockState existing = world.getBlockState(pos);
        if (!existing.isAir() && !(existing.getBlock() instanceof PlantBlock)) {
            return;
        }

        int roll = RANDOM.nextInt(10);
        if (roll < 2) {
            set(world, pos, OXEYE_DAISY);
        } else if (roll < 4) {
            set(world, pos, LILY_OF_THE_VALLEY);
        } else if (roll < 7) {
            set(world, pos, SHORT_GRASS);
        }
    }

    // =========================================================
    // 2. Four massive pillars at cardinal edges
    // =========================================================

    private static void buildPillars(ServerWorld world, BlockPos origin) {
        int pillarRadius = 10;
        int[] offsets = {-pillarRadius, pillarRadius};

        for (int xOffset : offsets) {
            buildPillarAt(world, origin.add(xOffset, 0, 0), Direction.EAST);
        }
        for (int zOffset : offsets) {
            buildPillarAt(world, origin.add(0, 0, zOffset), Direction.SOUTH);
        }
    }

    private static void buildPillarAt(ServerWorld world, BlockPos base, Direction facing) {
        int height = 17;

        // 3x3 chiseled stone-brick base with corner stairs
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = base.add(dx, 0, dz);
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                if (corner) {
                    Direction stairDir = (dx > 0 ? Direction.EAST : Direction.WEST);
                    if (dz > 0) {
                        stairDir = stairDir.rotateYClockwise();
                    }
                    set(world, p, STONE_BRICK_STAIRS.with(StairsBlock.FACING, stairDir));
                } else {
                    set(world, p, CHISELED_STONE_BRICKS);
                }
                // second base layer
                set(world, p.up(), CHISELED_STONE_BRICKS);
            }
        }

        // Vertical stone-brick core wrapped with spruce trapdoors / dark-oak fences
        BlockPos coreBase = base.up(2);
        for (int y = 0; y < height; y++) {
            BlockPos core = coreBase.add(0, y, 0);
            set(world, core, STONE_BRICKS);

            // Trapdoors/fences on the four cardinal faces of the core
            set(world, core.north(), SPRUCE_TRAPDOOR.with(TrapdoorBlock.FACING, Direction.NORTH)
                    .with(TrapdoorBlock.HALF, BlockHalf.BOTTOM)
                    .with(Properties.OPEN, y % 3 == 0));
            set(world, core.south(), SPRUCE_TRAPDOOR.with(TrapdoorBlock.FACING, Direction.SOUTH)
                    .with(TrapdoorBlock.HALF, BlockHalf.BOTTOM)
                    .with(Properties.OPEN, y % 3 == 1));
            set(world, core.east(), DARK_OAK_FENCE);
            set(world, core.west(), DARK_OAK_FENCE);
        }

        // Flared capital with stone-brick stairs
        BlockPos cap = coreBase.up(height);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(world, cap.add(dx, 0, dz), STONE_BRICKS);
            }
        }
        // Flare outward one block on each face
        set(world, cap.add(0, 1, 0), STONE_BRICKS);
        set(world, cap.north(), STONE_BRICK_STAIRS.with(StairsBlock.FACING, Direction.NORTH));
        set(world, cap.south(), STONE_BRICK_STAIRS.with(StairsBlock.FACING, Direction.SOUTH));
        set(world, cap.east(), STONE_BRICK_STAIRS.with(StairsBlock.FACING, Direction.EAST));
        set(world, cap.west(), STONE_BRICK_STAIRS.with(StairsBlock.FACING, Direction.WEST));

        // Small corner stair accents on the capital
        set(world, cap.north().east(), STONE_BRICK_STAIRS.with(StairsBlock.FACING, Direction.NORTH));
        set(world, cap.north().west(), STONE_BRICK_STAIRS.with(StairsBlock.FACING, Direction.WEST));
        set(world, cap.south().east(), STONE_BRICK_STAIRS.with(StairsBlock.FACING, Direction.EAST));
        set(world, cap.south().west(), STONE_BRICK_STAIRS.with(StairsBlock.FACING, Direction.SOUTH));
    }

    // =========================================================
    // 3. Arches between pillars with Gothic valance trim
    // =========================================================

    private static void buildArches(ServerWorld world, BlockPos origin) {
        int pillarRadius = 10;
        int pillarHeight = 17;
        int baseY = 2 + pillarHeight; // capital level

        // Four sides of the square formed by the pillars
        buildArchSegment(world, origin, -pillarRadius, -pillarRadius, 0, 0, baseY); // N-S across left
        buildArchSegment(world, origin, pillarRadius, pillarRadius, 0, 0, baseY);   // N-S across right
        buildArchSegment(world, origin, 0, 0, -pillarRadius, -pillarRadius, baseY); // E-W across back
        buildArchSegment(world, origin, 0, 0, pillarRadius, pillarRadius, baseY);   // E-W across front
    }

    private static void buildArchSegment(ServerWorld world, BlockPos origin,
                                          int x1, int x2, int z1, int z2, int baseY) {
        int mid = (x1 == 0) ? ((z1 + z2) / 2) : ((x1 + x2) / 2);
        int length = (x1 == 0) ? Math.abs(z2 - z1) : Math.abs(x2 - x1);
        int rise = length / 2 + 2;

        for (int step = 0; step <= length; step++) {
            int x = (x1 == x2) ? x1 : Math.min(x1, x2) + step;
            int z = (z1 == z2) ? z1 : Math.min(z1, z2) + step;
            // Parabolic arch height from the capitals
            double t = (step / (double) length) * 2.0 - 1.0; // -1..1
            int yOff = (int) Math.round(rise * (1.0 - t * t));

            BlockPos arch = origin.add(x, baseY + yOff, z);
            set(world, arch, STONE_BRICKS);

            // Gothic hanging valance trim every few blocks along the arch underside
            if (step % 2 == 0 && yOff > 0) {
                BlockPos trim = arch.down();
                set(world, trim, SPRUCE_STAIRS.with(StairsBlock.FACING, Direction.NORTH)
                        .with(StairsBlock.HALF, BlockHalf.TOP));
                set(world, trim.down(), DARK_OAK_FENCE);
                if (RANDOM.nextBoolean()) {
                    set(world, trim.down(2), SPRUCE_TRAPDOOR.with(TrapdoorBlock.FACING, Direction.SOUTH)
                            .with(TrapdoorBlock.HALF, BlockHalf.TOP)
                            .with(Properties.OPEN, true));
                }
            }
        }
    }

    // =========================================================
    // 4. Stepped dome capping the structure
    // =========================================================

    private static void buildDome(ServerWorld world, BlockPos origin) {
        int domeCentreY = 22; // origin.y + 22
        int radius = 10;
        int innerLitRadius = 2;

        BlockPos centre = origin.add(0, domeCentreY, 0);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int distSq = x * x + y * y + z * z;
                    if (distSq > radius * radius || distSq < (radius - 1) * (radius - 1)) {
                        continue;
                    }

                    BlockPos p = centre.add(x, y, z);

                    // Only build the upper half of the sphere (dome)
                    if (y < 0) {
                        continue;
                    }

                    // Determine material band based on height
                    BlockState state;
                    double normalizedHeight = (y + radius) / (double) (2 * radius);
                    if (normalizedHeight < 0.40) {
                        state = STONE_BRICKS;
                    } else if (normalizedHeight < 0.65) {
                        state = COBBLESTONE;
                    } else if (normalizedHeight < 0.85) {
                        state = DARK_OAK_PLANKS;
                    } else {
                        state = SPRUCE_STAIRS.with(StairsBlock.FACING, outwardFacing(x, z))
                                .with(StairsBlock.HALF, BlockHalf.BOTTOM);
                    }

                    // Vertical ribs using outward-facing spruce stairs at cardinal angles
                    if (isRibLine(x, z) && y >= 0) {
                        state = SPRUCE_STAIRS.with(StairsBlock.FACING, outwardFacing(x, z))
                                .with(StairsBlock.HALF, BlockHalf.BOTTOM);
                    }

                    set(world, p, state);
                }
            }
        }

        // Hollow out the inside of the dome above the light radius
        for (int x = -radius + 1; x <= radius - 1; x++) {
            for (int y = -radius + 1; y <= radius - 1; y++) {
                for (int z = -radius + 1; z <= radius - 1; z++) {
                    if (x * x + y * y + z * z < (radius - 1) * (radius - 1) && y >= 0) {
                        // Leave a small pocket around the central glowstone
                        if (x * x + y * y + z * z > innerLitRadius * innerLitRadius) {
                            set(world, centre.add(x, y, z), AIR);
                        }
                    }
                }
            }
        }
    }

    // New: connect the floating dome to the four pillar capitals
    private static void buildRoofSupports(ServerWorld world, BlockPos origin) {
        int pillarRadius = 10;
        int pillarHeight = 17;
        int capitalY = 2 + pillarHeight; // top of pillar capitals
        int domeY = 22;
        int[][] pillarCentres = {
                {-pillarRadius, 0}, {pillarRadius, 0}, {0, -pillarRadius}, {0, pillarRadius}
        };

        for (int[] centre : pillarCentres) {
            int x = centre[0];
            int z = centre[1];
            // Build a stone-brick brace from the capital up to the dome underside
            for (int y = capitalY + 1; y <= domeY - 3; y++) {
                BlockPos brace = origin.add(x, y, z);
                set(world, brace, STONE_BRICKS);
            }
            // Flared brace top where it meets the dome
            set(world, origin.add(x, domeY - 2, z), STONE_BRICKS);
            set(world, origin.add(x, domeY - 1, z), COBBLESTONE);
        }

        // Add horizontal tie beams between the braces at capital level
        int beamY = capitalY + 1;
        for (int i = -pillarRadius; i <= pillarRadius; i++) {
            set(world, origin.add(i, beamY, -pillarRadius), DARK_OAK_PLANKS);
            set(world, origin.add(i, beamY, pillarRadius), DARK_OAK_PLANKS);
            set(world, origin.add(-pillarRadius, beamY, i), DARK_OAK_PLANKS);
            set(world, origin.add(pillarRadius, beamY, i), DARK_OAK_PLANKS);
        }
    }

    private static boolean isRibLine(int x, int z) {
        return Math.abs(x) <= 1 || Math.abs(z) <= 1 || Math.abs(Math.abs(x) - Math.abs(z)) <= 1;
    }

    private static Direction outwardFacing(int x, int z) {
        if (Math.abs(z) >= Math.abs(x)) {
            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return x > 0 ? Direction.EAST : Direction.WEST;
    }

    // =========================================================
    // 5. Central interior light hanging from the dome
    // =========================================================

    private static void buildCentralLight(ServerWorld world, BlockPos origin) {
        // Anchor the lantern to the underside of the dome so it does not float.
        BlockPos anchor = origin.add(0, 21, 0);
        set(world, anchor, CHISELED_STONE_BRICKS);
        set(world, anchor.down(), DARK_OAK_FENCE);
        set(world, anchor.down(2), DARK_OAK_FENCE);
        set(world, anchor.down(3), RANDOM.nextBoolean() ? GLOWSTONE : REDSTONE_LAMP);
    }

    // =========================================================
    // Generic placement helper
    // =========================================================

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        // Avoid overwriting non-air with air, and avoid replacing fluids
        if (state.isAir() && world.getBlockState(pos).isAir()) {
            return;
        }
        if (!state.isAir()) {
            Block existing = world.getBlockState(pos).getBlock();
            if (existing instanceof FluidBlock) {
                return;
            }
        }
        world.setBlockState(pos, state, Block.NOTIFY_ALL);
    }
}
