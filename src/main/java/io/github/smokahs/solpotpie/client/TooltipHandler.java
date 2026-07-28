package io.github.smokahs.solpotpie.client;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.tracking.FoodInstance;
import io.github.smokahs.solpotpie.tracking.FoodList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static io.github.smokahs.solpotpie.lib.Localization.localized;
import static io.github.smokahs.solpotpie.lib.Localization.localizedComponent;
import static io.github.smokahs.solpotpie.lib.Localization.spelledNumber;

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
		if (!SOLPotPieConfig.hasTooltip(food)) return;

		List<Component> lines = buildLines(player, food);
		if (lines.isEmpty()) return;

		List<Component> tooltip = event.getToolTip();
		tooltip.addAll(insertionIndex(tooltip, food), lines);
	}

	private static List<Component> buildLines(Player player, Item food) {
		List<Component> lines = new ArrayList<>();

		if (!SOLPotPieConfig.isAllowed(food)) {
			lines.add(localizedTooltip("disabled", ChatFormatting.DARK_GRAY));
			return lines;
		}

		FoodList foodList = FoodList.get(player);
		if (!foodList.hasEverEaten(food)) {
			lines.add(localizedTooltip("not_yet_eaten", ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
			if (SOLPotPieConfig.shouldHideValuesUntilEaten()) {
				return lines;
			}
		}

		if (!Screen.hasShiftDown()) {
			lines.add(Component.literal(localized("gui", "food_book.queue.tooltip.hold_shift")));
			return lines;
		}

		double score = FoodList.getScore(new FoodInstance(food));
		lines.add(Component.literal(
				localized("gui", "food_book.queue.tooltip.score_label") + ": " + String.format("%.1f", score))
				.withStyle(ChatFormatting.GOLD));

		Component recency = recencyLine(foodList, food);
		if (recency != null) {
			lines.add(recency);
		}

		return lines;
	}

	@Nullable
	private static Component recencyLine(FoodList foodList, Item food) {
		int lastEaten = foodList.getLastEaten(food);
		if (lastEaten == -1) return null;

		String text;
		if (lastEaten == 0) {
			int inARow = Math.max(foodList.currentStreak(food), 1);
			if (inARow == 1) {
				text = localized("gui", "food_book.queue.tooltip.most_recent");
			} else if (inARow == 2) {
				text = localized("gui", "food_book.queue.tooltip.most_recent_twice");
			} else {
				text = localized("gui", "food_book.queue.tooltip.most_recent_streak", spelledNumber(inARow));
			}
		} else {
			String path = lastEaten == 1
					? "food_book.queue.tooltip.last_eaten_label_singular"
					: "food_book.queue.tooltip.last_eaten_label";
			text = localized("gui", path, spelledNumber(lastEaten));
		}

		return Component.literal(text).withStyle(ChatFormatting.GRAY);
	}

	private static int insertionIndex(List<Component> tooltip, Item food) {
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(food);
		if (id == null) return tooltip.size();

		String idText = id.toString();
		for (int i = 0; i < tooltip.size(); i++) {
			if (idText.equals(tooltip.get(i).getString())) {
				return i;
			}
		}
		return tooltip.size();
	}

	private static Component localizedTooltip(String path, ChatFormatting... formats) {
		return localizedComponent("tooltip", path).withStyle(formats);
	}

	private TooltipHandler() {}
}
