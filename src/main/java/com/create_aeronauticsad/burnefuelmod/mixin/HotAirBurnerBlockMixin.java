package com.create_aeronauticsad.burnefuelmod.mixin;

import com.create_aeronauticsad.burnefuelmod.FuelledBurnerAccess;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HotAirBurnerBlock.class)
public abstract class HotAirBurnerBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void createburnerfuel$handleFuelInteraction(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos,
                                                        final Player player, final InteractionHand hand, final BlockHitResult hitResult,
                                                        final CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!(level.getBlockEntity(pos) instanceof HotAirBurnerBlockEntity burnerBlockEntity)) {
            return;
        }

        final IItemHandler inventory = ((FuelledBurnerAccess) burnerBlockEntity).createburnerfuel$getFuelInventory();
        if (stack.isEmpty()) {
            if (!player.isShiftKeyDown()) {
                return;
            }

            final ItemStack extractedPreview = inventory.extractItem(0, 64, true);
            if (extractedPreview.isEmpty()) {
                return;
            }

            if (!level.isClientSide) {
                final ItemStack extracted = inventory.extractItem(0, 64, false);
                if (!player.addItem(extracted)) {
                    player.drop(extracted, false);
                }
            }

            cir.setReturnValue(ItemInteractionResult.SUCCESS);
            return;
        }

        if (HotAirBurnerBlock.Variant.getConversionFromItem(stack.getItem()) != null) {
            return;
        }

        if (!AbstractFurnaceBlockEntity.isFuel(stack)) {
            cir.setReturnValue(ItemInteractionResult.FAIL);
            return;
        }

        final ItemStack remaining = inventory.insertItem(0, stack.copy(), true);
        if (remaining.getCount() == stack.getCount()) {
            return;
        }

        if (!level.isClientSide) {
            final ItemStack insertedRemainder = inventory.insertItem(0, stack.copy(), false);
            final int inserted = stack.getCount() - insertedRemainder.getCount();
            if (!player.getAbilities().instabuild) {
                stack.shrink(inserted);
            }
        }

        cir.setReturnValue(ItemInteractionResult.SUCCESS);
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void createburnerfuel$refreshPoweredStateAfterRedstone(final BlockState state, final Level level, final BlockPos pos, final Block blockIn,
                                                                   final BlockPos fromPos, final boolean isMoving, final CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof final HotAirBurnerBlockEntity burnerBlockEntity) {
            ((FuelledBurnerAccess) burnerBlockEntity).createburnerfuel$refreshPoweredState();
        }
    }
}
