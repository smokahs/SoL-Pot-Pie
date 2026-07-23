package io.github.smokahs.solpotpie.integration;

import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.tracking.FoodList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.api.event.TooltipOverlayEvent;
import squeek.appleskin.api.food.FoodValues;

public final class AppleSkinCompat {
	private AppleSkinCompat() {}

	@SubscribeEvent
	public static void onFoodValues(FoodValuesEvent event) {
		Player player = event.player;
		if (player == null) return;

		ItemStack stack = event.itemStack;
		if (!stack.isEdible()) return;

		FoodValues shown = event.modifiedFoodValues;
		FoodList.MealValues meal =
				FoodList.get(player).diminish(stack.getItem(), shown.hunger, shown.getSaturationIncrement());
		if (meal.hunger() == shown.hunger && meal.saturation() == shown.getSaturationIncrement()) return;

		if (meal.hunger() <= 0) {
			event.modifiedFoodValues = new FoodValues(0, 0.0F);
			return;
		}

		event.modifiedFoodValues =
				new FoodValues(meal.hunger(), meal.saturation() / (meal.hunger() * 2.0F));
	}

	@SubscribeEvent
	public static void onTooltipOverlay(TooltipOverlayEvent.Pre event) {
		if (shouldHide(event.itemStack)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onHungerRestoredPreview(HUDOverlayEvent.HungerRestored event) {
		if (shouldHide(event.itemStack)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onHealthRestoredPreview(HUDOverlayEvent.HealthRestored event) {
		if (shouldHide(event.itemStack)) {
			event.setCanceled(true);
		}
	}

	private static boolean shouldHide(ItemStack stack) {
		if (!SOLPotPieConfig.shouldHideValuesUntilEaten()) return false;

		Player player = Minecraft.getInstance().player;
		if (player == null) return false;

		Item item = stack.getItem();
		if (!item.isEdible()) return false;

		return !FoodList.get(player).hasEverEaten(item);
	}
}
