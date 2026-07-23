package io.github.smokahs.solpotpie.book;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.ICustomComponent;

import java.util.ArrayList;
import java.util.function.UnaryOperator;
import java.util.List;

import static io.github.smokahs.solpotpie.lib.Localization.localized;

@OnlyIn(Dist.CLIENT)
public class UntastedComponent implements ICustomComponent {
	int columns = 6;
	int rows = 6;
	@SerializedName("show_empty_text") boolean showEmptyText = true;

	private transient int x;
	private transient int y;
	private transient FoodGrid grid;
	private transient List<ItemStack> foods = new ArrayList<>();

	@Override
	public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {}

	@Override
	public void build(int componentX, int componentY, int pageNum) {
		x = componentX;
		y = componentY;
		grid = new FoodGrid(columns, rows);
		grid.position(x, y);
	}

	@Override
	public void onDisplayed(IComponentRenderContext context) {
		foods = BookFoods.untasted();
		grid.reset();
	}

	@Override
	public void render(GuiGraphics graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
		if (foods.isEmpty()) {
			if (showEmptyText) {
				graphics.drawString(Minecraft.getInstance().font,
						Component.literal(localized("gui", "food_book.untasted.empty")).setStyle(context.getFont()),
						x, y, context.getHeaderColor(), false);
			}
			return;
		}

		grid.render(graphics, context, foods, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(IComponentRenderContext context, double mouseX, double mouseY, int mouseButton) {
		return !foods.isEmpty() && grid.mouseClicked(context, foods, mouseX, mouseY);
	}
}
