package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** scores every food from its own nutrition/saturation, log-compressed so filling foods can't run away */
public final class FoodScores {
	private FoodScores() {}

	public static Map<FoodInstance, Double> compute() {
		Map<FoodInstance, Double> scores = new HashMap<>();
		int foodCount = 0;
		for (Item item : ForgeRegistries.ITEMS) {
			if (!item.isEdible()) continue;
			scores.put(new FoodInstance(item), score(item));
			foodCount++;
		}

		SOLPotPie.LOGGER.info("Computed food scores for {} foods", foodCount);
		return scores;
	}

	/** linear ramp below avg 5, logarithmic compression above, x scoreMultiplier */
	public static double score(Item item) {
		FoodProperties food = item.getFoodProperties();
		if (food == null) return 0.0;

		double nutrition = food.getNutrition();
		// saturation counts once here (not the x2 restored amount) so fatty foods aren't over-weighted
		double saturation = nutrition * food.getSaturationModifier();
		double average = (nutrition + saturation) / 2.0;

		double raw;
		if (average < 5.0) {
			// weak foods ramp linearly from 0 up to 1
			raw = average / 5.0;
		} else {
			// strong foods compressed so nothing balloons past the top of the curve
			raw = 4.0 * Math.log10(average - 4.0) + 1.0;
		}
		return clamp(raw * SOLPotPieConfig.scoreMultiplier());
	}

	private static double clamp(double score) {
		return Math.max(0.0, Math.min(score, SOLPotPieConfig.maxScore()));
	}
}
