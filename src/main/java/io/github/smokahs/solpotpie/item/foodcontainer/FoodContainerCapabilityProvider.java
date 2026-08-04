package io.github.smokahs.solpotpie.item.foodcontainer;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FoodContainerCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
	private int slots;
	private final LazyOptional<ItemStackHandler> inventory = LazyOptional.of(() -> new ItemStackHandler(slots) {
		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return !(stack.getItem() instanceof FoodContainerItem) && super.isItemValid(slot, stack);
		}

		@Override
		public void deserializeNBT(CompoundTag nbt) {
			// saved "Size" wins in super; force configured size, keep what fits
			super.deserializeNBT(nbt);
			if (getSlots() != slots) {
				NonNullList<ItemStack> saved = stacks;
				setSize(slots);
				for (int i = 0; i < Math.min(saved.size(), slots); i++) {
					stacks.set(i, saved.get(i));
				}
			}
		}
	});

	public FoodContainerCapabilityProvider(ItemStack stack, int slots) {
		this.slots = slots;
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER)
			return inventory.cast();
		return LazyOptional.empty();
	}

	@Override
	public CompoundTag serializeNBT() {
		if (inventory.isPresent()) {
			return inventory.resolve().get().serializeNBT();
		}
		return new CompoundTag();
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		inventory.ifPresent(h -> h.deserializeNBT(nbt));
	}
}
