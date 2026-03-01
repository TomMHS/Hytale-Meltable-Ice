package de.tommhs.meltable.ice.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;

/**
 * Places fluids into the fluid layer (FluidSection).
 *
 * <p>This implementation resolves numeric fluid ids at runtime to avoid hardcoding build-dependent values.</p>
 */
@SuppressWarnings("SpellCheckingInspection")
public final class FluidPlacement {

    private static final String WATER_SOURCE_KEY = "Water_Source";
    private static final String LAVA_SOURCE_KEY = "Lava_Source";

    // 0 means empty; 1 is treated as "full" in your current setup.
    private static final byte WATER_LEVEL_FULL = 1;

    // Fallback only. Used if resolution fails (still better than "nothing happens").
    private static final int FALLBACK_WATER_SOURCE_FLUID_ID = 7;

    private static volatile int cachedWaterSourceFluidId = Integer.MIN_VALUE;

    private FluidPlacement() {
    }

    /**
     * Call once during plugin setup to log what ids the server resolves.
     */
    public static void logResolvedIds(@Nonnull HytaleLogger logger) {
        int water = FluidIdResolver.resolveFluidId(WATER_SOURCE_KEY);
        int lava = FluidIdResolver.resolveFluidId(LAVA_SOURCE_KEY);

        logger.atInfo().log("Resolved fluid ids: Water_Source=" + water + ", Lava_Source=" + lava);

        if (water == Integer.MIN_VALUE) {
            logger.atWarning().log("Could not resolve Water_Source fluid id. Using fallback=" + FALLBACK_WATER_SOURCE_FLUID_ID);
        }

        if (water != Integer.MIN_VALUE && lava != Integer.MIN_VALUE && water == lava) {
            logger.atWarning().log("Water_Source and Lava_Source resolved to the same id. Something is off with the resolver.");
        }
    }

    public static void placeWaterSource(@Nonnull World world, int worldX, int worldY, int worldZ) {
        int waterId = resolveWaterSourceFluidId();
        placeFluid(world, worldX, worldY, worldZ, waterId, WATER_LEVEL_FULL);
    }

    public static void placeFluid(@Nonnull World world, int worldX, int worldY, int worldZ, int fluidId, byte level) {
        byte safeLevel = (byte) Math.max(0, level);

        world.getChunkStore()
                .getChunkSectionReferenceAsync(
                        ChunkUtil.chunkCoordinate(worldX),
                        ChunkUtil.chunkCoordinate(worldY),
                        ChunkUtil.chunkCoordinate(worldZ)
                )
                .thenAcceptAsync(sectionRef -> {
                    Store<ChunkStore> sectionStore = sectionRef.getStore();

                    FluidSection fluidSection = sectionStore.getComponent(sectionRef, FluidSection.getComponentType());
                    if (fluidSection == null) {
                        return;
                    }

                    int blockIndex = ChunkUtil.indexBlock(worldX, worldY, worldZ);
                    fluidSection.setFluid(blockIndex, fluidId, safeLevel);

                    ChunkSection chunkSection = sectionStore.getComponent(sectionRef, ChunkSection.getComponentType());
                    if (chunkSection == null) {
                        return;
                    }

                    WorldChunk worldChunk = sectionStore.getComponent(
                            chunkSection.getChunkColumnReference(),
                            WorldChunk.getComponentType()
                    );
                    if (worldChunk == null) {
                        return;
                    }

                    worldChunk.markNeedsSaving();
                    worldChunk.setTicking(worldX, worldY, worldZ, true);
                }, world);
    }

    private static int resolveWaterSourceFluidId() {
        int cached = cachedWaterSourceFluidId;
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }

        int resolved = FluidIdResolver.resolveFluidId(WATER_SOURCE_KEY);
        if (resolved == Integer.MIN_VALUE) {
            cachedWaterSourceFluidId = FALLBACK_WATER_SOURCE_FLUID_ID;
            return FALLBACK_WATER_SOURCE_FLUID_ID;
        }

        cachedWaterSourceFluidId = resolved;
        return resolved;
    }
}