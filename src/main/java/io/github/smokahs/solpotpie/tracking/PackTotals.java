package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.ConfigHandler;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import net.minecraft.world.item.Item;

import java.util.Map;

public final class PackTotals {
	private static volatile int foodCount = -1;
	private static volatile double maxPoints = 0.0;

	private PackTotals() {}

	public static boolean counts(Item item) {
		Double score = ConfigHandler.scoreMap.get(new FoodInstance(item));
		return score != null && qualifies(item, score);
	}

	private static boolean qualifies(Item item, double score) {
		return score > 0.0 && item.isEdible() && SOLPotPieConfig.isAllowed(item);
	}

	public static void invalidate() {
		foodCount = -1;
	}

	private static void computeIfNeeded() {
		if (foodCount >= 0) return;

		int count = 0;
		double points = 0.0;
		for (Map.Entry<FoodInstance, Double> entry : ConfigHandler.scoreMap.entrySet()) {
			if (!qualifies(entry.getKey().getItem(), entry.getValue())) continue;

			count++;
			points += entry.getValue();
		}

		maxPoints = points;
		foodCount = count;
	}

	public static int foodCount() {
		computeIfNeeded();
		return foodCount;
	}

	public static double maxPoints() {
		computeIfNeeded();
		return maxPoints;
	}

	public static String maxPointsText() {
		return String.format("%.0f", maxPoints());
	}

	public static int maxHearts() {
		return HeartsHandler.heartsFromPoints(maxPoints());
	}
}
