package com.create_aeronauticsad.burnefuelmod.mixin;

import com.create_aeronauticsad.burnefuelmod.FuelledBurnerAccess;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
    @Inject(method = "onExplosionHit", at = @At("HEAD"))
    private void createburnerfuel$dropBurnerFuelOnExplosion(final BlockState state, final Level level, final BlockPos pos,
                                                            final Explosion explosion, final BiConsumer<ItemStack, BlockPos> dropConsumer,
                                                            final CallbackInfo ci) {
        if (!((Object) this instanceof HotAirBurnerBlock)) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof final HotAirBurnerBlockEntity burnerBlockEntity)) {
            return;
        }

        final ItemStack droppedFuel = ((FuelledBurnerAccess) burnerBlockEntity).createburnerfuel$takeFuelForDrop();
        if (!droppedFuel.isEmpty()) {
            dropConsumer.accept(droppedFuel, pos);
        }
    }
}
