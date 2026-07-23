package io.github.smokahs.solpotpie.book;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.patchouli.api.IComponentRenderContext;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class FoodGrid {
	static final int SLOT = 18;

	private final int columns;
	private final int rows;
	private final Pager pager = new Pager();

	private int x;
	private int y;

	FoodGrid(int columns, int rows) {
		this.columns = Math.max(1, columns);
		this.rows = Math.max(1, rows);
	}

	void position(int gridX, int gridY) {
		x = gridX;
		y = gridY;
	}

	void reset() {
		pager.reset();
	}

	int perPage() {
		return columns * rows;
	}

	void render(GuiGraphics graphics, IComponentRenderContext context, List<ItemStack> foods,
			int mouseX, int mouseY) {
		pager.clamp(foods.size(), perPage());

		int firstIndex = pager.page() * perPage();
		for (int i = 0; i < perPage() && firstIndex + i < foods.size(); i++) {
			context.renderItemStack(graphics,
					x + (i % columns) * SLOT, y + (i / columns) * SLOT,
					mouseX, mouseY, foods.get(firstIndex + i));
		}

		pager.render(graphics, context, x, pagerY(), columns * SLOT, foods.size(), perPage(), mouseX, mouseY);
	}

	boolean mouseClicked(IComponentRenderContext context, List<ItemStack> foods, double mouseX, double mouseY) {
		return pager.mouseClicked(context, x, pagerY(), columns * SLOT,
				foods.size(), perPage(), mouseX, mouseY);
	}

	private int pagerY() {
		return y + rows * SLOT + 2;
	}
}
