package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.ConfigHandler;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.api.FoodCapability;
import io.github.smokahs.solpotpie.api.SOLPotPieAPI;
import io.github.smokahs.solpotpie.foodgroups.FoodGroups;
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
	private static final String NBT_KEY_STREAK_FOOD = "streakFood";
	private static final String NBT_KEY_STREAK = "streak";
	private static final String NBT_KEY_REPEATS = "repeats";

	public record MealValues(int hunger, float saturation) {}

	/** why a food is not diminishing right now, or NONE if it is. */
	public enum DiminishingBlock {
		NONE,
		NEW_PLAYER,
		EXEMPT_FOOD,
		VARIED_DIET,
	}

	public static FoodList get(Player player) {
		return (FoodList) player.getCapability(SOLPotPieAPI.foodCapability)
			.orElseThrow(FoodListNotFoundException::new);
	}

	private static final int MAX_FOODS_EATEN = 1000;
	private int foodsEaten = 0;
	private final Map<FoodInstance, Integer> uniqueFoods = new HashMap<>();
	private final Set<FoodInstance> allTimeFoods = new HashSet<>();
	private final Map<FoodInstance, Integer> repeats = new HashMap<>();
	@Nullable
	private FoodInstance streakFood = null;
	private int streak = 0;
	private int cachedDistinctGroups = -1;
	private int cachedGroupsGeneration = -1;

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
		tag.putInt(NBT_KEY_REPEATS, repeats.getOrDefault(food, 0));

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

		if (streakFood != null) {
			String encodedStreakFood = streakFood.encode();
			if (encodedStreakFood != null) {
				tag.putString(NBT_KEY_STREAK_FOOD, encodedStreakFood);
				tag.putInt(NBT_KEY_STREAK, streak);
			}
		}

		return tag;
	}

	/** used for persistent storage */
	@Override
	public void deserializeNBT(CompoundTag tag) {
		ListTag list = tag.getList(NBT_KEY_FOOD_LIST, Tag.TAG_COMPOUND);

		uniqueFoods.clear();
		repeats.clear();
		list.stream()
			.map(nbt-> (CompoundTag) nbt)
			.forEach(nbt -> {
				FoodInstance uniqueFood = FoodInstance.decode(nbt.getString(NBT_KEY_UNIQUE_FOOD));
				if (uniqueFood == null) return;

				uniqueFoods.put(uniqueFood, Math.round(nbt.getFloat(NBT_KEY_LAST_EATEN)));
				int repeatCount = nbt.getInt(NBT_KEY_REPEATS);
				if (repeatCount > 0) {
					repeats.put(uniqueFood, repeatCount);
				}
			});
		foodsEaten = tag.getInt(NBT_KEY_FOODS_EATEN);

		allTimeFoods.clear();
		ListTag allTimeList = tag.getList(NBT_KEY_ALL_TIME_FOODS, Tag.TAG_STRING);
		allTimeList.stream()
			.map(Tag::getAsString)
			.map(FoodInstance::decode)
			.filter(Objects::nonNull)
			.forEach(allTimeFoods::add);

		streakFood = tag.contains(NBT_KEY_STREAK_FOOD, Tag.TAG_STRING)
			? FoodInstance.decode(tag.getString(NBT_KEY_STREAK_FOOD))
			: null;
		streak = streakFood == null ? 0 : tag.getInt(NBT_KEY_STREAK);

		invalidateFoodGroups();
	}

	public void addFood(Item food, Map<FoodInstance, Integer> foodMap) {
		if (!SOLPotPieConfig.shouldCount(food) && !SOLPotPieConfig.shouldForbiddenCount()) {
			return;
		}

		if (foodsEaten < MAX_FOODS_EATEN) {
			foodsEaten++;
		}

		FoodInstance newlyEaten = new FoodInstance(food);
		int carriedEats = effectiveEats(newlyEaten);

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
			repeats.remove(foodInstance);
		}

		streak = newlyEaten.equals(streakFood) ? streak + 1 : 1;
		streakFood = newlyEaten;

		if (SOLPotPieConfig.shouldCount(food)) {
			foodMap.put(newlyEaten, 0);
			allTimeFoods.add(newlyEaten);
			repeats.put(newlyEaten, carriedEats + 1);
		}

		invalidateFoodGroups();
	}

	private int effectiveEats(FoodInstance food) {
		int priorEats = repeats.getOrDefault(food, 0);
		if (priorEats <= 0) return 0;

		Integer mealsSince = uniqueFoods.get(food);
		if (mealsSince == null) return 0;

		double recovered = Math.min(1.0, (mealsSince + 1) / (double) SOLPotPieConfig.size()
				* SOLPotPieConfig.diminishingRecoveryVal());
		return (int) Math.round(priorEats * (1.0 - recovered));
	}
	public DiminishingBlock diminishingBlock(Item food) {
		int graceThreshold = SOLPotPieConfig.newPlayerFoodsEatenThreshold();
		if (graceThreshold > 0 && foodsEaten < graceThreshold) {
			return DiminishingBlock.NEW_PLAYER;
		}

		if (FoodGroups.isExempt(food)) {
			return DiminishingBlock.EXEMPT_FOOD;
		}

		int diversityThreshold = SOLPotPieConfig.foodGroupDiversityThreshold();
		if (diversityThreshold > 0 && distinctFoodGroups() > diversityThreshold) {
			return DiminishingBlock.VARIED_DIET;
		}

		return DiminishingBlock.NONE;
	}

	public int distinctFoodGroups() {
		int generation = FoodGroups.generation();
		if (cachedDistinctGroups >= 0 && cachedGroupsGeneration == generation) {
			return cachedDistinctGroups;
		}

		List<Item> recent = new ArrayList<>(uniqueFoods.size());
		for (FoodInstance food : uniqueFoods.keySet()) {
			recent.add(food.getItem());
		}

		cachedDistinctGroups = FoodGroups.distinctGroups(recent);
		cachedGroupsGeneration = generation;
		return cachedDistinctGroups;
	}

	private void invalidateFoodGroups() {
		cachedDistinctGroups = -1;
	}

	public MealValues diminish(Item food, int nutrition, float saturation) {
		MealValues full = new MealValues(nutrition, saturation);
		if (!SOLPotPieConfig.diminishingReturnsEnabled()) return full;
		if (diminishingBlock(food) != DiminishingBlock.NONE) return full;

		int priorEats = effectiveEats(new FoodInstance(food));
		if (priorEats <= 0) return full;

		int eatsToFloor = Math.max(2, SOLPotPieConfig.diminishingEatsToFloor());
		double slide = Math.min(1.0, priorEats / (double) (eatsToFloor - 1));

		int floorHunger = SOLPotPieConfig.diminishingFloorHunger();
		float floorSaturation = (float) SOLPotPieConfig.diminishingFloorSaturation();

		int diminishedHunger = (int) Math.round(nutrition + (floorHunger - nutrition) * slide);
		float diminishedSaturation = (float) (saturation + (floorSaturation - saturation) * slide);

		return new MealValues(
				Math.min(diminishedHunger, nutrition), Math.min(diminishedSaturation, saturation));
	}

	public int timesEaten(Item food) {
		return effectiveEats(new FoodInstance(food));
	}

	public int currentStreak(Item food) {
		if (streakFood == null) return 0;
		return streakFood.equals(new FoodInstance(food)) ? streak : 0;
	}

	public void addFood(Item food) {
		addFood(food, uniqueFoods);
	}

	@Override
	public double lifetimePoints() {
		double points = 0;
		for (FoodInstance food : allTimeFoods) {
			points += getScore(food);
		}
		return points;
	}

	public int discoveredFoods() {
		int discovered = 0;
		for (FoodInstance food : allTimeFoods) {
			if (PackTotals.counts(food.getItem())) {
				discovered++;
			}
		}
		return discovered;
	}

	@Override
	public double foodDiversity() {
		return uniqueFoods.size();
	}

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

		return FoodScores.score(food.item);
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

	public void clearFood() {
		uniqueFoods.clear();
		allTimeFoods.clear();
		repeats.clear();
		streakFood = null;
		streak = 0;
		invalidateFoodGroups();
	}

	public void clearRecent() {
		uniqueFoods.clear();
		repeats.clear();
		streakFood = null;
		streak = 0;
		invalidateFoodGroups();
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
