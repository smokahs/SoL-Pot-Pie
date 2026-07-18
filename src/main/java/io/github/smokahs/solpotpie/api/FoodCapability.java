package io.github.smokahs.solpotpie.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

/**
Provides a stable, (strongly) simplified view of the food list.
 */
public interface FoodCapability extends ICapabilitySerializable<CompoundTag> {
	/** @return whether the given food is in the recent-food queue */
	boolean hasEaten(Item item);

	/** @return whether the given food has ever been eaten by this player */
	boolean hasEverEaten(Item item);

	/** @return the number of distinct foods in the recent-food queue */
	double foodDiversity();

	/** @return the sum of the scores of every food this player has ever eaten */
	double lifetimePoints();
}
