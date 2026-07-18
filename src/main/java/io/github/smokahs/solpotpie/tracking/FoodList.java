package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.ConfigHandler;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.api.FoodCapability;
import io.github.smokahs.solpotpie.api.SOLPotPieAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
public final class FoodList implements FoodCapability {
	private static final String NBT_KEY_FOOD_LIST = "foodList";
	private static final String NBT_KEY_UNIQUE_FOOD = "food";
	private static final String NBT_KEY_LAST_EATEN = "lastEaten";
	private static final String NBT_KEY_FOODS_EATEN = "foodsEaten";
	private static final String NBT_KEY_ALL_TIME_FOODS = "allTimeFoods";

	public static FoodList get(Player player) {
		return (FoodList) player.getCapability(SOLPotPieAPI.foodCapability)
			.orElseThrow(FoodListNotFoundException::new);
	}

	private static final int MAX_FOODS_EATEN = 1000;
	private int foodsEaten = 0;
	// Keys - foods eaten recently, Values - lastEaten, i.e. # meals ago the food was last eaten
	private final Map<FoodInstance, Integer> uniqueFoods = new HashMap<>();
	// every food ever eaten; each contributes its score to lifetime points exactly once
	private final Set<FoodInstance> allTimeFoods = new HashSet<>();

	public FoodList() {}

	private final LazyOptional<FoodList> capabilityOptional = LazyOptional.of(() -> this);

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
		return capability == SOLPotPieAPI.foodCapability ? capabilityOptional.cast() : LazyOptional.empty();
	}

	@Nullable
	private CompoundTag serializeUniqueFood(Map.Entry<FoodInstance, Integer> foodPair) {
		FoodInstance food = foodPair.getKey();
		Integer lastEaten = foodPair.getValue();

		String encodedFood = food.encode();
		if (encodedFood == null) {
			return null;
		}

		CompoundTag tag = new CompoundTag();
		StringTag s = StringTag.valueOf(encodedFood);
		FloatTag i = FloatTag.valueOf(lastEaten);
		tag.put(NBT_KEY_UNIQUE_FOOD, s);
		tag.put(NBT_KEY_LAST_EATEN, i);

		return tag;
	}

	/** used for persistent storage */
	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();

		ListTag list = new ListTag();

		uniqueFoods.entrySet().stream()
			.map(this::serializeUniqueFood)
			.filter(Objects::nonNull)
			.forEach(list::add);
		tag.put(NBT_KEY_FOOD_LIST, list);
		tag.put(NBT_KEY_FOODS_EATEN, IntTag.valueOf(foodsEaten));

		ListTag allTimeList = new ListTag();
		allTimeFoods.stream()
			.map(FoodInstance::encode)
			.filter(Objects::nonNull)
			.map(StringTag::valueOf)
			.forEach(allTimeList::add);
		tag.put(NBT_KEY_ALL_TIME_FOODS, allTimeList);

		return tag;
	}

	@Nullable
	private Pair<FoodInstance, Integer> deserializeUniqueFood(Pair<String, Float> encoded) {
		FoodInstance uniqueFood = FoodInstance.decode(encoded.getKey());
		Integer lastEaten = Math.round(encoded.getValue());

		if (uniqueFood == null)
			return null;

		return new ImmutablePair<>(uniqueFood, lastEaten);
	}

	/** used for persistent storage */
	@Override
	public void deserializeNBT(CompoundTag tag) {
		ListTag list = tag.getList(NBT_KEY_FOOD_LIST, Tag.TAG_COMPOUND);

		uniqueFoods.clear();
		list.stream()
			.map(nbt-> (CompoundTag) nbt)
			.map(nbt -> new ImmutablePair<>(nbt.getString(NBT_KEY_UNIQUE_FOOD), nbt.getFloat(NBT_KEY_LAST_EATEN)))
			.map(this::deserializeUniqueFood)
			.filter(Objects::nonNull)
			.forEach(pair -> uniqueFoods.put(pair.getKey(), pair.getValue()));
		foodsEaten = tag.getInt(NBT_KEY_FOODS_EATEN);

		allTimeFoods.clear();
		ListTag allTimeList = tag.getList(NBT_KEY_ALL_TIME_FOODS, Tag.TAG_STRING);
		allTimeList.stream()
			.map(Tag::getAsString)
			.map(FoodInstance::decode)
			.filter(Objects::nonNull)
			.forEach(allTimeFoods::add);
	}

	public void addFood(Item food, Map<FoodInstance, Integer> foodMap) {
		if (!SOLPotPieConfig.shouldCount(food) && !SOLPotPieConfig.shouldForbiddenCount()) {
			return;
		}

		if (foodsEaten < MAX_FOODS_EATEN) {
			foodsEaten++;
		}

		ArrayList<FoodInstance> toRemove = new ArrayList<>();

		for (Map.Entry<FoodInstance, Integer> entry : foodMap.entrySet()) {
			FoodInstance foodInstance = entry.getKey();
			Integer lastEaten = entry.getValue();

			lastEaten++;
			foodMap.put(foodInstance, lastEaten);

			if (lastEaten >= SOLPotPieConfig.size()) {
				toRemove.add(foodInstance);
			}
		}

		for (FoodInstance foodInstance : toRemove) {
			foodMap.remove(foodInstance);
		}

		if (SOLPotPieConfig.shouldCount(food)) {
			FoodInstance newlyEaten = new FoodInstance(food);
			foodMap.put(newlyEaten, 0);
			allTimeFoods.add(newlyEaten);
		}
	}

	public void addFood(Item food) {
		addFood(food, uniqueFoods);
	}

	/** sum of the scores of every food ever eaten; only ever goes up */
	@Override
	public double lifetimePoints() {
		double points = 0;
		for (FoodInstance food : allTimeFoods) {
			points += getScore(food);
		}
		return points;
	}

	/** @return the number of distinct foods in the recent-food queue */
	@Override
	public double foodDiversity() {
		return uniqueFoods.size();
	}

	/** lunchbox ranking: new-to-you foods first (highest score), then absent from queue, then least recently eaten */
	public double rankFood(Item food) {
		if (!SOLPotPieConfig.shouldCount(food)) {
			return -1;
		}

		FoodInstance foodInstance = new FoodInstance(food);
		if (!allTimeFoods.contains(foodInstance)) {
			return 20000 + getScore(foodInstance);
		}

		Integer lastEaten = uniqueFoods.get(foodInstance);
		if (lastEaten == null) {
			return 10000 + getScore(foodInstance);
		}

		return lastEaten;
	}

	public static double getScore(FoodInstance food) {
		if (ConfigHandler.scoreMap != null && ConfigHandler.scoreMap.containsKey(food)) {
			return ConfigHandler.scoreMap.get(food);
		}

		return FoodScores.nutritionBase(food.item);
	}

	public Set<Map.Entry<FoodInstance, Integer>> getData() {
		return uniqueFoods.entrySet();
	}

	public int getLastEaten(Item food) {
		if (!hasEaten(food)) {
			return -1;
		}

		return uniqueFoods.get(new FoodInstance(food));
	}

	@Override
	public boolean hasEaten(Item food) {
		if (!food.isEdible()) return false;
		return uniqueFoods.containsKey(new FoodInstance(food));
	}

	@Override
	public boolean hasEverEaten(Item food) {
		if (!food.isEdible()) return false;
		return allTimeFoods.contains(new FoodInstance(food));
	}

	/** full reset: recent queue, lifetime foods, and meal counter */
	public void clearFood() {
		uniqueFoods.clear();
		allTimeFoods.clear();
	}

	/** clears only the recent-food queue; lifetime points untouched */
	public void clearRecent() {
		uniqueFoods.clear();
	}

	public Set<FoodInstance> getEatenFoods() {
		return uniqueFoods.keySet();
	}

	public int getFoodsEaten() {
		return foodsEaten;
	}

	public void resetFoodsEaten() {
		foodsEaten = 0;
	}

	public static class FoodListNotFoundException extends RuntimeException {
		public FoodListNotFoundException() {
			super("Player must have food capability attached, but none was found.");
		}
	}
}
