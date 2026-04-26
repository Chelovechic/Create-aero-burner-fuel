package com.create_aeronauticsad.burnefuelmod;

import com.mojang.logging.LogUtils;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(createburnerfuel.MODID)
public final class createburnerfuel {
    public static final String MODID = "createburnerfuel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public createburnerfuel(final IEventBus modEventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AeroBlockEntityTypes.HOT_AIR_BURNER.get(),
                (blockEntity, side) -> ((FuelledBurnerAccess) blockEntity).createburnerfuel$getFuelInventory()
        );
    }
}
