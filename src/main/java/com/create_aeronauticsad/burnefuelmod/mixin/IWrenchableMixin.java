package com.create_aeronauticsad.burnefuelmod.mixin;

import com.create_aeronauticsad.burnefuelmod.FuelledBurnerAccess;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IWrenchable.class)
public interface IWrenchableMixin {
    @Inject(method = "onSneakWrenched", at = @At("HEAD"))
    private void createburnerfuel$dropBurnerFuelOnSneakWrench(final BlockState state, final UseOnContext context,
                                                              final CallbackInfoReturnable<InteractionResult> cir) {
        if (!((Object) this instanceof HotAirBurnerBlock)) {
            return;
        }

        final Level level = context.getLevel();
        if (level.isClientSide) {
            return;
        }

        final BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof final HotAirBurnerBlockEntity burnerBlockEntity)) {
            return;
        }

        final ItemStack droppedFuel = ((FuelledBurnerAccess) burnerBlockEntity).createburnerfuel$takeFuelForDrop();
        if (!droppedFuel.isEmpty()) {
            Containers.dropContents(level, pos, new SimpleContainer(droppedFuel));
        }
    }
}
