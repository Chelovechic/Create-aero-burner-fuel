package com.create_aeronauticsad.burnefuelmod.mixin;

import com.create_aeronauticsad.burnefuelmod.FuelledBurnerAccess;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void createburnerfuel$dropBurnerFuelOnPlayerDestroy(final Level level, final BlockPos pos, final BlockState state,
                                                                final Player player, final CallbackInfoReturnable<BlockState> cir) {
        if (level.isClientSide || !((Object) this instanceof HotAirBurnerBlock)) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof final HotAirBurnerBlockEntity burnerBlockEntity)) {
            return;
        }

        final ItemStack droppedFuel = ((FuelledBurnerAccess) burnerBlockEntity).createburnerfuel$takeFuelForDrop();
        if (droppedFuel.isEmpty()) {
            return;
        }

        Containers.dropContents(level, pos, new SimpleContainer(droppedFuel));
    }
}
