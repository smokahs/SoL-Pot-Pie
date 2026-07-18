package io.github.smokahs.solpotpie.client.gui.elements;

import io.github.smokahs.solpotpie.tracking.FoodInstance;
import io.github.smokahs.solpotpie.tracking.FoodList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static io.github.smokahs.solpotpie.lib.Localization.localized;


/** renders a food from the recent queue; tooltip shows its score and how recently it was eaten */
public class UIFoodQueueItem extends UIItemStack{
	private final int lastEaten;

	public UIFoodQueueItem(ItemStack itemStack, int lastEaten) {
		super(itemStack);

		this.lastEaten = lastEaten;
	}

	@Override
	protected void renderTooltip(GuiGraphics matrices, int mouseX, int mouseY) {
		List<Component> tooltip = getFoodQueueTooltip();
		renderTooltip(matrices, itemStack, tooltip, mouseX, mouseY);
	}

	private List<Component> getFoodQueueTooltip() {
		Component foodName =  Component.translatable(itemStack.getItem().getDescriptionId(itemStack))
				.withStyle(itemStack.getRarity().color);

		List<Component> tooltip = new ArrayList<>();
		tooltip.add(foodName);

		Component space = Component.literal("");
		tooltip.add(space);

		double score = FoodList.getScore(new FoodInstance(itemStack.getItem()));
		tooltip.add(Component.literal(
				localized("gui", "food_book.queue.tooltip.score_label") + ": " + String.format("%.1f", score))
				.withStyle(ChatFormatting.GOLD));

		String lastEatenPath = lastEaten == 1
				? "food_book.queue.tooltip.last_eaten_label_singular"
				: "food_book.queue.tooltip.last_eaten_label";
		tooltip.add(Component.literal(localized("gui", lastEatenPath, lastEaten)).withStyle(ChatFormatting.GRAY));

		return tooltip;
	}
}
