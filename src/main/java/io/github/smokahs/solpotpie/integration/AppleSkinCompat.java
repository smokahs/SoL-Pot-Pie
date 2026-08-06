package io.github.smokahs.solpotpie.integration;

import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.item.foodcontainer.FoodContainerItem;
import io.github.smokahs.solpotpie.tracking.FoodList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.ItemStackHandler;
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

		Item eaten = stack.getItem();
		FoodValues shown = event.modifiedFoodValues;
		if (eaten instanceof FoodContainerItem) {
			// container's own props are 0/0; preview the food finishUsingItem would pick
			ItemStack next = nextMeal(stack, player);
			FoodProperties props = next.isEmpty() ? null : next.getFoodProperties(player);
			if (props == null) return;
			eaten = next.getItem();
			shown = new FoodValues(props.getNutrition(), props.getSaturationModifier());
			event.modifiedFoodValues = shown;
		}

		FoodList.MealValues meal =
				FoodList.get(player).diminish(eaten, shown.hunger, shown.getSaturationIncrement());
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
		if (item instanceof FoodContainerItem) {
			ItemStack next = nextMeal(stack, player);
			if (next.isEmpty()) return false;
			item = next.getItem();
		}
		if (!item.isEdible()) return false;

		return !FoodList.get(player).hasEverEaten(item);
	}

	private static ItemStack nextMeal(ItemStack container, Player player) {
		ItemStackHandler handler = FoodContainerItem.getInventory(container);
		if (handler == null) return ItemStack.EMPTY;

		int slot = FoodContainerItem.getBestFoodSlot(handler, player);
		return slot < 0 ? ItemStack.EMPTY : handler.getStackInSlot(slot);
	}
}
