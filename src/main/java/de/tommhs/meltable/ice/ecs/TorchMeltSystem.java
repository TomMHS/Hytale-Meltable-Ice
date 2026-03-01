package de.tommhs.meltable.ice.ecs;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.tommhs.meltable.ice.world.IceMelter;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * ECS event system that listens for block placements and melts nearby custom ice blocks
 * when a heat source (torch/campfire/fire) is placed.
 *
 * <p>Implementation notes:</p>
 * <ul>
 *   <li>Runs the melt operation via {@link World#execute(Runnable)} to ensure it happens after placement is applied.</li>
 *   <li>Heat source detection is based on the placed block key string.</li>
 * </ul>
 */
@SuppressWarnings("SpellCheckingInspection")
public final class TorchMeltSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int MELT_RADIUS_BLOCKS = 6;
    private static final String EMPTY_BLOCK_KEY = "Empty";

    public TorchMeltSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PlaceBlockEvent event
    ) {
        if (!shouldProcess(event)) {
            return;
        }

        String placedBlockKey = getPlacedBlockKey(event);
        if (!isHeatSourceBlock(placedBlockKey)) {
            return;
        }

        World world = resolveWorld(index, chunk, store);
        if (world == null) {
            return;
        }

        Vector3i heatSourcePos = event.getTargetBlock();

        world.execute(() -> {
            int melted = IceMelter.meltNearbyIceToWater(world, heatSourcePos, MELT_RADIUS_BLOCKS);
            logIfMelted(melted, heatSourcePos, placedBlockKey);
        });
    }

    /**
     * We don't require any specific components to receive this ECS event.
     */
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    private static boolean shouldProcess(@Nonnull PlaceBlockEvent event) {
        if (event.isCancelled()) {
            return false;
        }

        ItemStack item = event.getItemInHand();
        return item != null && !item.isEmpty();
    }

    /**
     * Extracts the block key that is being placed.
     *
     * @return A non-null string. If no useful key is present, returns {@code "Empty"}.
     */
    @Nonnull
    private static String getPlacedBlockKey(@Nonnull PlaceBlockEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.isEmpty()) {
            return EMPTY_BLOCK_KEY;
        }

        String blockKey = item.getBlockKey();
        if (blockKey == null) {
            return EMPTY_BLOCK_KEY;
        }

        String trimmed = blockKey.trim();
        return trimmed.isEmpty() ? EMPTY_BLOCK_KEY : trimmed;
    }

    /**
     * Resolves the world from the player component stored on the entity that triggered the event.
     *
     * @return The world instance or {@code null} if not available.
     */
    private static World resolveWorld(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store
    ) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return null;
        }

        return player.getWorld();
    }

    /**
     * Determines whether a placed block should be treated as a heat source.
     *
     * <p>This uses a simple substring match to keep it robust against different asset ids.</p>
     */
    private static boolean isHeatSourceBlock(@Nonnull String blockKey) {
        if (EMPTY_BLOCK_KEY.equals(blockKey)) {
            return false;
        }

        String k = blockKey.toLowerCase(Locale.ROOT);
        return k.contains("torch") || k.contains("campfire") || k.contains("fire");
    }

    private static void logIfMelted(int melted, @Nonnull Vector3i pos, @Nonnull String heatSourceKey) {
        if (melted <= 0) {
            return;
        }

        LOGGER.atInfo().log(
                "Melted " + melted + " Tommhs_Rock_Ice blocks near heat source '" + heatSourceKey + "' at " + pos
        );
    }
}