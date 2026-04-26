package com.create_aeronauticsad.burnefuelmod.mixin;

import com.create_aeronauticsad.burnefuelmod.Config;
import com.create_aeronauticsad.burnefuelmod.FuelledBurnerAccess;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.util.AeroSoundDistUtil;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(HotAirBurnerBlockEntity.class)
public abstract class HotAirBurnerBlockEntityMixin extends SmartBlockEntity implements FuelledBurnerAccess {
    @Shadow protected int signalStrength;
    @Shadow protected ScrollValueBehaviour hotAirAmountBehaviour;
    @Shadow private int maxCapacity;
    @Shadow protected double lastRenderTime;
    @Shadow protected double renderTime;
    @Shadow protected int ticksSinceSync;
    @Shadow public dev.eriksonn.aeronautics.content.blocks.hot_air.GasEmitterRenderHandler renderHandler;
    @Shadow protected LerpedFloat intensity;

    @Unique
    private final ItemStackHandler createburnerfuel$fuelInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return AbstractFurnaceBlockEntity.isFuel(stack);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 64;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            if (HotAirBurnerBlockEntityMixin.this.createburnerfuel$suspendInventoryCallbacks) {
                return;
            }

            HotAirBurnerBlockEntityMixin.this.createburnerfuel$markDirtyAndSync();
        }
    };

    @Unique
    private double createburnerfuel$remainingFuelTicks;

    @Unique
    private boolean createburnerfuel$suspendInventoryCallbacks;

    protected HotAirBurnerBlockEntityMixin(final BlockEntityType<?> type, final BlockPos pos, final BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public IItemHandler createburnerfuel$getFuelInventory() {
        return this.createburnerfuel$fuelInventory;
    }

    @Override
    public ItemStack createburnerfuel$takeFuelForDrop() {
        final ItemStack storedFuel = this.createburnerfuel$fuelInventory.getStackInSlot(0);
        if (storedFuel.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final ItemStack droppedFuel = storedFuel.copy();
        this.createburnerfuel$suspendInventoryCallbacks = true;
        try {
            this.createburnerfuel$fuelInventory.setStackInSlot(0, ItemStack.EMPTY);
        } finally {
            this.createburnerfuel$suspendInventoryCallbacks = false;
        }
        this.setChanged();
        return droppedFuel;
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;tick()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void createburnerfuel$disableClientVisualsWithoutFuel(final CallbackInfo ci) {
        if (this.level == null || !this.level.isClientSide || this.createburnerfuel$hasFuelForOutput()) {
            return;
        }

        this.ticksSinceSync++;
        this.intensity.chase(0.0D, 0.1D, LerpedFloat.Chaser.EXP);
        this.intensity.tickChaser();
        this.lastRenderTime = this.renderTime;
        this.renderTime += 1.0D / 20.0D;
        this.renderHandler.targetFromRedstoneSignal(0);
        this.renderHandler.tick();

        if (!this.isVirtual()) {
            AeroSoundDistUtil.removePosHotAirBurnerSound(this.getBlockPos());
        }

        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void createburnerfuel$consumeFuel(final CallbackInfo ci) {
        if (this.level == null || this.level.isClientSide || this.signalStrength <= 0 || this.isRemoved()) {
            return;
        }

        if (!this.createburnerfuel$ensureFuelLoaded()) {
            this.createburnerfuel$refreshPoweredState();
            return;
        }

        final double outputFraction = this.createburnerfuel$getOutputFraction();
        if (outputFraction <= 0.0D) {
            return;
        }

        final double burnRate = Config.BURN_RATE_MULTIPLIER.get() * outputFraction;
        if (burnRate <= 0.0D) {
            return;
        }

        final double previousFuel = this.createburnerfuel$remainingFuelTicks;
        this.createburnerfuel$remainingFuelTicks = Math.max(0.0D, this.createburnerfuel$remainingFuelTicks - burnRate);
        if ((previousFuel > 0.0D) != (this.createburnerfuel$remainingFuelTicks > 0.0D)) {
            this.createburnerfuel$refreshPoweredState();
            this.sendData();
        }
        this.setChanged();
    }

    @Inject(method = "canOutputGas", at = @At("HEAD"), cancellable = true)
    private void createburnerfuel$requireFuel(final CallbackInfoReturnable<Boolean> cir) {
        if (this.level != null && !this.level.isClientSide && this.signalStrength > 0) {
            this.createburnerfuel$ensureFuelLoaded();
        }
        cir.setReturnValue(this.signalStrength > 0 && !this.isRemoved() && this.createburnerfuel$remainingFuelTicks > 0.0D);
    }

    @Inject(method = "getGasOutput", at = @At("HEAD"), cancellable = true)
    private void createburnerfuel$gateGasOutput(final CallbackInfoReturnable<Double> cir) {
        if (this.level != null && !this.level.isClientSide && this.signalStrength > 0) {
            this.createburnerfuel$ensureFuelLoaded();
        }

        if (!this.createburnerfuel$hasFuelForOutput()) {
            cir.setReturnValue(0.0D);
        }
    }

    @Inject(method = "getSignalStrength", at = @At("HEAD"), cancellable = true)
    private void createburnerfuel$hideSignalWhenOutOfFuel(final CallbackInfoReturnable<Integer> cir) {
        if (!this.createburnerfuel$hasFuelForOutput()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void createburnerfuel$writeFuelData(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        compound.put("FuelInventory", this.createburnerfuel$fuelInventory.serializeNBT(registries));
        compound.putDouble("RemainingFuelTicks", this.createburnerfuel$remainingFuelTicks);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void createburnerfuel$readFuelData(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        if (compound.contains("FuelInventory")) {
            this.createburnerfuel$suspendInventoryCallbacks = true;
            try {
                this.createburnerfuel$fuelInventory.deserializeNBT(registries, compound.getCompound("FuelInventory"));
            } finally {
                this.createburnerfuel$suspendInventoryCallbacks = false;
            }
        }
        this.createburnerfuel$remainingFuelTicks = compound.getDouble("RemainingFuelTicks");
    }

    @Inject(method = "addToGoggleTooltip", at = @At("TAIL"), cancellable = true)
    private void createburnerfuel$appendFuelTooltip(final List<Component> tooltip, final boolean isPlayerSneaking, final CallbackInfoReturnable<Boolean> cir) {
        final ItemStack stack = this.createburnerfuel$fuelInventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            CreateLang.text("Fuel").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CreateLang.text("0").style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " items").forGoggles(tooltip, 1);
        } else {
            CreateLang.builder()
                    .add(CreateLang.number(stack.getCount()).style(ChatFormatting.GOLD))
                    .text(ChatFormatting.GRAY, " ")
                    .add(stack.getHoverName().copy().withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }

        cir.setReturnValue(true);
    }

    @Unique
    private boolean createburnerfuel$ensureFuelLoaded() {
        if (this.createburnerfuel$hasFuelForOutput()) {
            return true;
        }

        final ItemStack stack = this.createburnerfuel$fuelInventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            return false;
        }

        final int burnTime = AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        if (burnTime <= 0) {
            return false;
        }

        final Item fuelItem = stack.getItem();
        this.createburnerfuel$suspendInventoryCallbacks = true;
        try {
            stack.shrink(1);
            if (stack.isEmpty()) {
                final Item remainder = fuelItem.getCraftingRemainingItem();
                this.createburnerfuel$fuelInventory.setStackInSlot(0, remainder == null ? ItemStack.EMPTY : new ItemStack(remainder));
            } else {
                this.createburnerfuel$fuelInventory.setStackInSlot(0, stack);
            }
        } finally {
            this.createburnerfuel$suspendInventoryCallbacks = false;
        }

        this.createburnerfuel$remainingFuelTicks += burnTime;
        this.createburnerfuel$markDirtyAndSync();
        return true;
    }

    @Unique
    private boolean createburnerfuel$hasFuelForOutput() {
        return this.createburnerfuel$remainingFuelTicks > 0.0D;
    }

    @Unique
    private void createburnerfuel$markDirtyAndSync() {
        this.setChanged();
        this.createburnerfuel$refreshPoweredState();
        this.sendData();
    }

    @Override
    public void createburnerfuel$refreshPoweredState() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        final BlockState state = this.getBlockState();
        if (!state.hasProperty(HotAirBurnerBlock.POWERED)) {
            return;
        }

        final boolean shouldBePowered = this.signalStrength > 0 && this.createburnerfuel$hasFuelForOutput();
        if (state.getValue(HotAirBurnerBlock.POWERED) != shouldBePowered) {
            this.level.setBlock(this.worldPosition, state.setValue(HotAirBurnerBlock.POWERED, shouldBePowered), 2);
        }
    }

    @Unique
    private double createburnerfuel$getOutputFraction() {
        if (this.hotAirAmountBehaviour == null || this.maxCapacity <= 0) {
            return 0.0D;
        }

        final double hotAirFraction = this.hotAirAmountBehaviour.value / (double) this.maxCapacity;
        final double signalFraction = this.signalStrength / 15.0D;
        return Math.max(0.0D, hotAirFraction) * Math.max(0.0D, signalFraction);
    }
}
