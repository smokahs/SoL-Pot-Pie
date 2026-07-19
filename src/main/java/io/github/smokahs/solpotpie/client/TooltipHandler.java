package io.github.smokahs.solpotpie.client;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.tracking.FoodInstance;
import io.github.smokahs.solpotpie.tracking.FoodList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

import static io.github.smokahs.solpotpie.lib.Localization.localized;
import static io.github.smokahs.solpotpie.lib.Localization.localizedComponent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLPotPie.MOD_ID)
public final class TooltipHandler {
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (!SOLPotPieConfig.isFoodTooltipEnabled()) return;

		Player player = event.getEntity();
		if (player == null) return;

		Item food = event.getItemStack().getItem();
		if (!food.isEdible()) return;

		FoodList foodList = FoodList.get(player);
		boolean isAllowed = SOLPotPieConfig.isAllowed(food);

		List<Component> tooltip = event.getToolTip();
		if (!isAllowed) {
			tooltip.add(localizedTooltip("disabled", ChatFormatting.DARK_GRAY));
			return;
		}

		if (!foodList.hasEverEaten(food)) {
			tooltip.add(localizedTooltip("not_yet_eaten", ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
			if (SOLPotPieConfig.shouldHideValuesUntilEaten()) {
				return;
			}
		}

		double score = FoodList.getScore(new FoodInstance(food));
		tooltip.add(Component.literal(
				localized("gui", "food_book.queue.tooltip.score_label") + ": " + String.format("%.1f", score))
				.withStyle(ChatFormatting.GOLD));

		int lastEaten = foodList.getLastEaten(food);
		if (lastEaten != -1) {
			String lastEatenPath = "food_book.queue.tooltip.last_eaten_label";
			if (lastEaten == 1) {
				lastEatenPath = "food_book.queue.tooltip.last_eaten_label_singular";
			}
			tooltip.add(Component.literal(localized("gui", lastEatenPath, lastEaten)).withStyle(ChatFormatting.GRAY));
		}
	}

	private static Component localizedTooltip(String path, ChatFormatting... formats) {
		return localizedComponent("tooltip", path).withStyle(formats);
	}

	private TooltipHandler() {}
}
