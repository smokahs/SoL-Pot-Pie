package io.github.smokahs.solpotpie.book;

import io.github.smokahs.solpotpie.ConfigHandler;
import io.github.smokahs.solpotpie.tracking.FoodInstance;
import io.github.smokahs.solpotpie.tracking.FoodList;
import io.github.smokahs.solpotpie.tracking.PackTotals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class BookFoods {
	private BookFoods() {}

	public static List<ItemStack> recent() {
		List<ItemStack> stacks = new ArrayList<>();

		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return stacks;
		}

		List<Map.Entry<FoodInstance, Integer>> data = new ArrayList<>(FoodList.get(player).getData());
		data.sort(Comparator.comparingInt(Map.Entry::getValue));
		for (Map.Entry<FoodInstance, Integer> entry : data) {
			stacks.add(new ItemStack(entry.getKey().getItem()));
		}
		return stacks;
	}

	public static List<ItemStack> untasted() {
		List<ItemStack> stacks = new ArrayList<>();

		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return stacks;
		}

		FoodList foodList = FoodList.get(player);
		List<Map.Entry<FoodInstance, Double>> remaining = new ArrayList<>();
		for (Map.Entry<FoodInstance, Double> entry : ConfigHandler.scoreMap.entrySet()) {
			Item item = entry.getKey().getItem();
			if (PackTotals.counts(item) && !foodList.hasEverEaten(item)) {
				remaining.add(entry);
			}
		}

		remaining.sort(Map.Entry.<FoodInstance, Double>comparingByValue().reversed());
		for (Map.Entry<FoodInstance, Double> entry : remaining) {
			stacks.add(new ItemStack(entry.getKey().getItem()));
		}
		return stacks;
	}
}
