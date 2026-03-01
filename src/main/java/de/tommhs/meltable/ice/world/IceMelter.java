package de.tommhs.meltable.ice.world;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import javax.annotation.Nonnull;

/**
 * Utility that converts the custom meltable ice block into water when triggered by a heat source.
 *
 * <p>Melting means:</p>
 * <ol>
 *   <li>Replace the ice block with {@code "Empty"} (air)</li>
 *   <li>Place {@code "Water_Source"} as a block at the same position</li>
 * </ol>
 *
 * <p>This class only performs the conversion. It does not decide when to melt.
 * That is handled by the ECS system that detects heat source placement.</p>
 */
@SuppressWarnings("SpellCheckingInspection")
public final class IceMelter {

    /**
     * Asset id of the custom block that should melt.
     * Keep this in sync with the filename / asset key of your item/block JSON.
     */
    private static final String ICE_BLOCK_ID = "Tommhs_Meltable_Rock_Ice";

    /**
     * World height limit used to clamp Y scanning.
     * Prefer a world/dimension API if available.
     */
    private static final int MAX_WORLD_Y = 319;

    private IceMelter() {
    }

    /**
     * Scans a cube around {@code center} and melts all matching ice blocks into water.
     * Only loaded chunks are processed.
     *
     * @param world The world to modify.
     * @param center The center position of the scan.
     * @param radiusBlocks The scan radius in blocks.
     * @return The number of blocks that were converted.
     */
    public static int meltNearbyIceToWater(@Nonnull World world, @Nonnull Vector3i center, int radiusBlocks) {
        Bounds bounds = Bounds.around(center, radiusBlocks);
        return meltWithinBounds(world, bounds);
    }

    private static int meltWithinBounds(@Nonnull World world, @Nonnull Bounds b) {
        int melted = 0;

        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                melted += meltColumnIfChunkLoaded(world, x, z, b.minY(), b.maxY());
            }
        }

        return melted;
    }

    private static int meltColumnIfChunkLoaded(@Nonnull World world, int x, int z, int minY, int maxY) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return 0;
        }

        int melted = 0;

        for (int y = minY; y <= maxY; y++) {
            if (meltBlockIfIce(world, x, y, z)) {
                melted++;
            }
        }

        return melted;
    }

    private static boolean meltBlockIfIce(@Nonnull World world, int x, int y, int z) {
        BlockType type = world.getBlockType(x, y, z);
        if (type == null) {
            return false;
        }

        if (!ICE_BLOCK_ID.equals(type.getId())) {
            return false;
        }

        // Replace the solid block with air first.
        world.setBlock(x, y, z, BlockType.EMPTY_KEY);

        // Place water source directly as a block/fluid source.
        FluidPlacement.placeWaterSource(world, x, y, z);

        return true;
    }

    /**
     * Simple axis-aligned bounds container for scan operations.
     */
    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {

        static Bounds around(@Nonnull Vector3i center, int r) {
            int minX = center.x - r;
            int maxX = center.x + r;

            int minY = Math.max(0, center.y - r);
            int maxY = Math.min(MAX_WORLD_Y, center.y + r);

            int minZ = center.z - r;
            int maxZ = center.z + r;

            return new Bounds(minX, maxX, minY, maxY, minZ, maxZ);
        }
    }
}