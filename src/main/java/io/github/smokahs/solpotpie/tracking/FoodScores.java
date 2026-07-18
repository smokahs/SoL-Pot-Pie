package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** assigns every food a score: crafted = sum of food ingredients' scores x multiplier / output count, floor = nutrition base */
public final class FoodScores {
	private FoodScores() {}

	public static Map<FoodInstance, Double> compute(MinecraftServer server) {
		RegistryAccess registryAccess = server.registryAccess();
		double craftMultiplier = SOLPotPieConfig.craftMultiplier();

		Map<Item, List<Recipe<?>>> recipesByResult = new HashMap<>();
		for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
			ItemStack result;
			try {
				result = recipe.getResultItem(registryAccess);
			} catch (Exception e) {
				continue; // some modded recipes can't resolve a static result
			}
			if (result == null || result.isEmpty() || !result.getItem().isEdible()) continue;
			recipesByResult.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(recipe);
		}

		Map<Item, Double> memo = new HashMap<>();
		Map<FoodInstance, Double> scores = new HashMap<>();
		int foodCount = 0;
		for (Item item : ForgeRegistries.ITEMS) {
			if (!item.isEdible()) continue;
			scores.put(new FoodInstance(item),
					score(item, recipesByResult, memo, new HashSet<>(), registryAccess, craftMultiplier));
			foodCount++;
		}

		SOLPotPie.LOGGER.info("Computed food scores for {} foods from {} food recipes",
				foodCount, recipesByResult.size());
		return scores;
	}

	private static double score(Item item, Map<Item, List<Recipe<?>>> recipesByResult,
			Map<Item, Double> memo, Set<Item> visiting, RegistryAccess registryAccess, double craftMultiplier) {
		Double cached = memo.get(item);
		if (cached != null) return cached;
		// recipe cycle (e.g. two foods craftable from each other): fall back to nutrition
		if (!visiting.add(item)) return nutritionBase(item);

		double best = nutritionBase(item);
		for (Recipe<?> recipe : recipesByResult.getOrDefault(item, List.of())) {
			double ingredientSum = 0;
			boolean anyFoodIngredient = false;
			for (Ingredient ingredient : recipe.getIngredients()) {
				double cheapest = Double.MAX_VALUE;
				for (ItemStack match : ingredient.getItems()) {
					if (match.isEmpty() || !match.getItem().isEdible()) continue;
					cheapest = Math.min(cheapest,
							score(match.getItem(), recipesByResult, memo, visiting, registryAccess, craftMultiplier));
				}
				if (cheapest != Double.MAX_VALUE) {
					ingredientSum += cheapest;
					anyFoodIngredient = true;
				}
			}
			if (!anyFoodIngredient) continue;

			int outputCount = Math.max(1, recipe.getResultItem(registryAccess).getCount());
			best = Math.max(best, ingredientSum * craftMultiplier / outputCount);
		}
		visiting.remove(item);

		best = clamp(best);
		memo.put(item, best);
		return best;
	}

	/** score for a food with no visible recipe: average of nutrition and saturation, scaled down */
	public static double nutritionBase(Item item) {
		FoodProperties food = item.getFoodProperties();
		if (food == null) return 0.0;

		double nutrition = food.getNutrition();
		double saturation = nutrition * food.getSaturationModifier() * 2.0;
		return clamp((nutrition + saturation) / 2.0 / SOLPotPieConfig.nutritionDivisor());
	}

	private static double clamp(double score) {
		return Math.max(0.0, Math.min(score, SOLPotPieConfig.maxScore()));
	}
}
