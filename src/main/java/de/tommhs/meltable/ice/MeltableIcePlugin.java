package de.tommhs.meltable.ice;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import de.tommhs.meltable.ice.ecs.TorchMeltSystem;
import de.tommhs.meltable.ice.world.FluidPlacement;

import javax.annotation.Nonnull;

/**
 * Main plugin entry point for the IceMelt mod.
 *
 * <p>This plugin registers an ECS {@link com.hypixel.hytale.component.system.EntityEventSystem}
 * that listens to {@link com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent} and
 * melts nearby custom ice blocks into water when a heat source (e.g. torch) is placed.</p>
 */
public final class MeltableIcePlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public MeltableIcePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log(prefix() + "Setting up");
        int water = BlockType.getAssetMap().getIndex("Water_Source");
        int lava = BlockType.getAssetMap().getIndex("Lava_Source");
        LOGGER.atInfo().log("Fluid block indices: Water_Source=" + water + ", Lava_Source=" + lava);

        FluidPlacement.logResolvedIds(LOGGER);

        // Block events are ECS events in current server builds.
        // Therefore we register an EntityEventSystem instead of a normal event listener.
        getEntityStoreRegistry().registerSystem(new TorchMeltSystem());

        LOGGER.atInfo().log(prefix() + "Registered TorchMeltSystem");
    }

    private String prefix() {
        return getName() + " v" + getManifest().getVersion() + " - ";
    }
}