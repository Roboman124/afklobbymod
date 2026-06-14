package com.example.afklobbymod.structure;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

/**
 * Generates a self-contained town-square theater / open-air stage.
 *
 * The build is centered on the supplied {@code origin} and extends roughly
 * 15 blocks wide (X) by 11 blocks deep (Z):
 *  - Flat stone/cobblestone ground plane.
 *  - Ground-level audience seating: two loose concentric rows of spruce-stair
 *    chairs facing the stage in a semi-circle.
 *  - Raised stage (1.5 blocks above ground) built from spruce slabs and solid
 *    blocks, with an asymmetrical jagged front lip made of spruce slabs, and
 *    dark-oak / spruce plank flooring.
 *  - Two 5-block stripped dark-oak/spruce log pillars flanking the stage,
 *    connected by a forward-jutting layered roof arch of horizontal logs,
 *    spruce stairs and slabs.
 *  - Backstage wall of black wool, plus left-side oak leaves cluster, hanging
 *    gold bell, small grass patch, and right-side chest and red bed on the
 *    stage floor.
 *  - Center-left floor inlay and exactly two campfires placed one block below
 *    the stage surface so only smoke particles rise through non-solid slab holes.
 */
public final class TownSquareTheaterGenerator {

    private static final Random RANDOM = new Random();

    // ---------- Block convenience constants ----------

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState STONE = Blocks.STONE.getDefaultState();
    private static final BlockState COBBLESTONE = Blocks.COBBLESTONE.getDefaultState();
    private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState SPRUCE_PLANKS = Blocks.SPRUCE_PLANKS.getDefaultState();
    private static final BlockState DARK_OAK_PLANKS = Blocks.DARK_OAK_PLANKS.getDefaultState();
    private static final BlockState SPRUCE_LOG = Blocks.SPRUCE_LOG.getDefaultState();
    private static final BlockState STRIPPED_SPRUCE_LOG = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState();
    private static final BlockState STRIPPED_DARK_OAK_LOG = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
    private static final BlockState DARK_OAK_LOG = Blocks.DARK_OAK_LOG.getDefaultState();
    private static final BlockState SPRUCE_SLAB = Blocks.SPRUCE_SLAB.getDefaultState();
    private static final BlockState SPRUCE_STAIRS = Blocks.SPRUCE_STAIRS.getDefaultState();
    private static final BlockState DARK_OAK_SLAB = Blocks.DARK_OAK_SLAB.getDefaultState();
    private static final BlockState DARK_OAK_STAIRS = Blocks.DARK_OAK_STAIRS.getDefaultState();
    private static final BlockState BLACK_WOOL = Blocks.BLACK_WOOL.getDefaultState();
    private static final BlockState BLACK_CONCRETE = Blocks.BLACK_CONCRETE.getDefaultState();
    private static final BlockState OAK_LEAVES = Blocks.OAK_LEAVES.getDefaultState();
    private static final BlockState GRASS = Blocks.GRASS.getDefaultState();
    private static final BlockState CAMPFIRE = Blocks.CAMPFIRE.getDefaultState();
    private static final BlockState CHEST = Blocks.CHEST.getDefaultState();
    private static final BlockState BELL = Blocks.BELL.getDefaultState();
    private static final BlockState BED = Blocks.RED_BED.getDefaultState();

    private TownSquareTheaterGenerator() {
        // Utility class
    }

    /**
     * Builds the entire Town Square Theater centered on {@code origin}.
     * The origin is roughly the center of the audience/ground plane.
     */
    public static void build(ServerWorld world, BlockPos origin) {
        buildGround(world, origin);
        buildAudienceSeating(world, origin);
        buildStage(world, origin);
        buildPillarsAndRoof(world, origin);
        buildBackstageWall(world, origin);
        buildStageDetails(world, origin);
        buildFloorInlayAndCampfires(world, origin);
    }

    // =========================================================
    // 1. Flat stone/cobblestone ground plane
    // =========================================================

    private static void buildGround(ServerWorld world, BlockPos origin) {
        int halfWidth = 7; // total width 15 (x: -7 .. 7)
        int halfDepth = 5; // total depth 11 (z: -5 .. 5)

        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int z = -halfDepth; z <= halfDepth; z++) {
                BlockPos pos = origin.add(x, 0, z);
                // Slight checker / noise pattern using stone/cobblestone
                BlockState ground = ((x + z) % 2 == 0) ? STONE : COBBLESTONE;
                if (RANDOM.nextInt(12) == 0) {
                    ground = STONE_BRICKS;
                }
                set(world, pos, ground);
                // Clear anything above the ground plane so the theater is open
                clearColumn(world, pos.up(), 8);
            }
        }
    }

    // =========================================================
    // 2. Audience seating: two concentric semi-circular rows
    // =========================================================

    private static void buildAudienceSeating(ServerWorld world, BlockPos origin) {
        // Audience sits in front of the stage; stage is at +Z.
        // Chairs face SOUTH toward the stage.
        Direction faceStage = Direction.SOUTH;

        // Inner row of 6 chairs, about z = -1 (2 blocks from stage apron)
        int[] innerX = {-5, -3, -1, 1, 3, 5};
        for (int x : innerX) {
            BlockPos seat = origin.add(x, 0, -1);
            set(world, seat, SPRUCE_STAIRS.with(StairsBlock.FACING, faceStage));
        }

        // Outer row of 5 chairs offset, about z = -3
        int[] outerX = {-5, -2, 0, 2, 5};
        for (int x : outerX) {
            BlockPos seat = origin.add(x, 0, -3);
            set(world, seat, SPRUCE_STAIRS.with(StairsBlock.FACING, faceStage));
        }
    }

    // =========================================================
    // 3. Raised stage platform
    // =========================================================

    private static void buildStage(ServerWorld world, BlockPos origin) {
        // Stage extends from z = +1 to +5 and x = -5 to +5.
        // Ground is y = 0; stage floor is y = +1 (1.5 blocks above ground
        // because the lower slab at y=+1 is a half-block, making the surface
        // 1.5 blocks above the ground plane).
        int minX = -5;
        int maxX = 5;
        int minZ = 1;
        int maxZ = 5;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos slabPos = origin.add(x, 1, z);

                // Solid support underneath the slab so the stage is not hollow
                set(world, slabPos.down(), SPRUCE_PLANKS);

                // Main stage surface: spruce slabs (bottom) so actors stand at
                // 1.5 blocks above ground.
                BlockState surface = SPRUCE_SLAB.with(Properties.SLAB_TYPE, SlabType.BOTTOM);

                // Flooring inlay pattern: mix of dark oak and spruce planks
                int pattern = (Math.abs(x) + Math.abs(z)) % 3;
                if (pattern == 0) {
                    surface = DARK_OAK_PLANKS;
                } else if (pattern == 2) {
                    surface = SPRUCE_PLANKS;
                }

                set(world, slabPos, surface);
            }
        }

        // Asymmetrical jagged/curved front lip of spruce slabs.
        // Lip sits one half-step lower than the stage surface, i.e. at y=0 top
        // half, jutting toward the audience (+Z direction from stage edge).
        BlockState topSlab = SPRUCE_SLAB.with(Properties.SLAB_TYPE, SlabType.TOP);
        int[][] lipOffsets = {
            {-5, 1}, {-4, 2}, {-3, 2}, {-2, 3}, {-1, 3},
            {0, 3}, {1, 3}, {2, 2}, {3, 2}, {4, 2}, {5, 1}
        };
        for (int[] pair : lipOffsets) {
            int x = pair[0];
            int forward = pair[1]; // how far the lip juts toward the audience
            for (int f = 1; f <= forward; f++) {
                BlockPos lipPos = origin.add(x, 0, 1 + f); // start just in front of stage
                set(world, lipPos, topSlab);
            }
        }
    }

    // =========================================================
    // 4. Pillars and forward-jutting roof arch/awning
    // =========================================================

    private static void buildPillarsAndRoof(ServerWorld world, BlockPos origin) {
        // Pillars stand at the back corners of the stage, flanking it.
        // Stage floor is y=1; pillars rise from ground y=0 up to y=4.
        int pillarZ = 5;
        int[] pillarX = {-4, 4};

        for (int x : pillarX) {
            BlockPos base = origin.add(x, 0, pillarZ);
            // 5-block tall stripped log pillars (y = 0..4)
            for (int y = 0; y < 5; y++) {
                set(world, base.up(y), STRIPPED_DARK_OAK_LOG);
            }
            // Small decorative foot at bottom
            set(world, base.down(), COBBLESTONE);
        }

        // Roof arch connecting the pillars, layered forward over the stage.
        // Starts at y = 5 (just above the 5-block pillars) and curves forward.
        int roofY = 5;
        int startZ = pillarZ;
        int endZ = 2; // juts forward toward the audience

        for (int x = -4; x <= 4; x++) {
            // Main horizontal dark-oak log beam across the back
            set(world, origin.add(x, roofY, startZ), DARK_OAK_LOG.with(Properties.AXIS, Direction.Axis.X));
        }

        // Layered canopy extending forward: logs, stairs, slabs
        for (int zStep = 0; zStep <= (startZ - endZ); zStep++) {
            int z = startZ - zStep;
            int y = roofY - (zStep / 2); // gentle downward slope toward front

            // Center dark-oak log spine
            set(world, origin.add(0, y, z), DARK_OAK_LOG.with(Properties.AXIS, Direction.Axis.Z));

            // Side supports / slope with spruce stairs facing the audience
            for (int x = -3; x <= 3; x++) {
                if (x == 0) continue;
                BlockState state;
                if (zStep % 2 == 0) {
                    state = SPRUCE_STAIRS.with(StairsBlock.FACING, Direction.SOUTH);
                } else {
                    state = SPRUCE_SLAB.with(Properties.SLAB_TYPE, SlabType.BOTTOM);
                }
                set(world, origin.add(x, y, z), state);
            }
        }

        // Small hanging spruce-stair accents at the front lip of the awning
        BlockPos awningFront = origin.add(0, roofY - 1, endZ);
        set(world, awningFront, SPRUCE_STAIRS.with(StairsBlock.FACING, Direction.NORTH).with(StairsBlock.HALF, BlockHalf.TOP));
        set(world, awningFront.west(), SPRUCE_STAIRS.with(StairsBlock.FACING, Direction.NORTH).with(StairsBlock.HALF, BlockHalf.TOP));
        set(world, awningFront.east(), SPRUCE_STAIRS.with(StairsBlock.FACING, Direction.NORTH).with(StairsBlock.HALF, BlockHalf.TOP));
    }

    // =========================================================
    // 5. Backstage wall of black wool / black concrete
    // =========================================================

    private static void buildBackstageWall(ServerWorld world, BlockPos origin) {
        // Back wall at the rear of the stage (z = 5) rising up behind the roof.
        int wallZ = 5;
        int wallYBase = 2; // starts above stage floor
        int wallHeight = 5;

        for (int x = -5; x <= 5; x++) {
            for (int y = 0; y < wallHeight; y++) {
                BlockState wall = ((x + y) % 3 == 0) ? BLACK_CONCRETE : BLACK_WOOL;
                set(world, origin.add(x, wallYBase + y, wallZ), wall);
            }
        }
    }

    // =========================================================
    // 6. Stage details: leaves, bell, grass, chest, bed
    // =========================================================

    private static void buildStageDetails(ServerWorld world, BlockPos origin) {
        // Stage floor surface is at y = 1.
        int stageY = 1;

        // Left side cluster of oak leaves (2-3 blocks high) near left pillar.
        BlockPos leavesBase = origin.add(-3, stageY + 1, 4);
        set(world, leavesBase, OAK_LEAVES);
        set(world, leavesBase.up(), OAK_LEAVES);
        set(world, leavesBase.east(), OAK_LEAVES);
        set(world, leavesBase.up().east(), OAK_LEAVES);
        if (RANDOM.nextBoolean()) {
            set(world, leavesBase.up(2), OAK_LEAVES);
        }

        // Gold bell hanging from the left pillar.
        BlockPos bellPos = origin.add(-4, 4, 5);
        set(world, bellPos, BELL.with(Properties.HANGING, true)
                .with(HorizontalFacingBlock.FACING, Direction.SOUTH));

        // Small grass patch on stage floor (center-left).
        set(world, origin.add(-2, stageY + 1, 3), GRASS);
        set(world, origin.add(-1, stageY + 1, 3), GRASS);
        set(world, origin.add(-2, stageY + 1, 2), GRASS);

        // Chest near right edge of stage.
        BlockPos chestPos = origin.add(4, stageY + 1, 3);
        set(world, chestPos, CHEST.with(HorizontalFacingBlock.FACING, Direction.WEST));

        // Red bed flat on stage floor near right edge, facing toward audience.
        BlockPos bedFoot = origin.add(3, stageY + 1, 4);
        BlockPos bedHead = bedFoot.south();
        set(world, bedFoot, BED.with(Properties.BED_PART, BedPart.FOOT)
                .with(HorizontalFacingBlock.FACING, Direction.SOUTH));
        set(world, bedHead, BED.with(Properties.BED_PART, BedPart.HEAD)
                .with(HorizontalFacingBlock.FACING, Direction.SOUTH));
    }

    // =========================================================
    // 7. Center-left floor inlay and exactly two campfires
    // =========================================================

    private static void buildFloorInlayAndCampfires(ServerWorld world, BlockPos origin) {
        // Center-left 1x1 stone/cobblestone inlay on the stage floor.
        BlockPos inlay = origin.add(-1, 1, 3);
        set(world, inlay, COBBLESTONE);

        // Exactly 2 campfires, placed one block below the stage floor (y = 0),
        // directly under non-solid stage surface holes so smoke rises through.
        // The stage floor above them uses spruce slabs (bottom) which are
        // non-solid and allow smoke particles to be seen.
        BlockPos campfire1 = origin.add(-1, 0, 3);
        BlockPos campfire2 = origin.add(1, 0, 3);

        // Ensure a non-solid block (bottom slab) directly above each campfire.
        BlockPos hole1 = campfire1.up();
        BlockPos hole2 = campfire2.up();

        // Remove any solid flooring above the campfire and replace with a
        // bottom slab so smoke can rise through but the stage surface won't burn.
        set(world, hole1, SPRUCE_SLAB.with(Properties.SLAB_TYPE, SlabType.BOTTOM));
        set(world, hole2, SPRUCE_SLAB.with(Properties.SLAB_TYPE, SlabType.BOTTOM));

        // Place unlit campfires one block below the stage surface.
        // Using Properties.LIT=false prevents fire from spreading or damaging
        // the wooden stage; only smoke particles will rise.
        BlockState safeCampfire = CAMPFIRE.with(Properties.LIT, true)
                .with(Properties.SIGNAL_FIRE, false)
                .with(CampfireBlock.FACING, Direction.SOUTH);
        set(world, campfire1, safeCampfire);
        set(world, campfire2, safeCampfire);
    }

    // =========================================================
    // Generic placement helpers
    // =========================================================

    private static void clearColumn(ServerWorld world, BlockPos start, int height) {
        for (int i = 0; i < height; i++) {
            BlockPos pos = start.up(i);
            if (!world.getBlockState(pos).isAir()) {
                set(world, pos, AIR);
            }
        }
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        // Avoid overwriting non-air with air and avoid redundant air -> air
        if (state.isAir() && world.getBlockState(pos).isAir()) {
            return;
        }
        world.setBlockState(pos, state, Block.NOTIFY_ALL);
    }
}
