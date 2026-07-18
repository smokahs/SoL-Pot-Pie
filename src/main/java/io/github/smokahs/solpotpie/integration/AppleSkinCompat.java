package io.github.smokahs.solpotpie.integration;

import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.tracking.FoodList;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.api.event.TooltipOverlayEvent;

/** registered only when appleskin is loaded; hides its food value displays for foods never eaten */
public final class AppleSkinCompat {
	private AppleSkinCompat() {}

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
