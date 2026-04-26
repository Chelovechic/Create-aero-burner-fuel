package com.create_aeronauticsad.burnefuelmod;

import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.world.item.ItemStack;

public interface FuelledBurnerAccess {
    IItemHandler createburnerfuel$getFuelInventory();

    ItemStack createburnerfuel$takeFuelForDrop();

    void createburnerfuel$refreshPoweredState();
}
