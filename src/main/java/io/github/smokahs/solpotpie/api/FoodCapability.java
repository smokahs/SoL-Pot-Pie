package io.github.smokahs.solpotpie.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public interface FoodCapability extends ICapabilitySerializable<CompoundTag> {
	boolean hasEaten(Item item);

	boolean hasEverEaten(Item item);

	double foodDiversity();

	double lifetimePoints();
}
