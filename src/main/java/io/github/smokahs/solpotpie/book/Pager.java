package io.github.smokahs.solpotpie.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.patchouli.api.IComponentRenderContext;

import static io.github.smokahs.solpotpie.lib.Localization.localized;

@OnlyIn(Dist.CLIENT)
final class Pager {
	private static final int DISABLED_ARROW = 0x808080;
	private static final String PREVIOUS = "<";
	private static final String NEXT = ">";
	private static final int ARROW_HEIGHT = 10;

	private int page;

	int page() {
		return page;
	}

	void reset() {
		page = 0;
	}

	static int pageCount(int size, int perPage) {
		return Math.max(1, (size + perPage - 1) / perPage);
	}

	void clamp(int size, int perPage) {
		page = Math.min(page, pageCount(size, perPage) - 1);
	}

	void render(GuiGraphics graphics, IComponentRenderContext context, int x, int y, int width,
			int size, int perPage, int mouseX, int mouseY) {
		int count = pageCount(size, perPage);
		if (count <= 1) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		String label = localized("gui", "food_book.fraction", page + 1, count);
		graphics.drawString(font, Component.literal(label).setStyle(context.getFont()),
				x + (width - font.width(label)) / 2, y, context.getTextColor(), false);

		drawArrow(graphics, context, font, PREVIOUS, x, y, mouseX, mouseY, page > 0);
		drawArrow(graphics, context, font, NEXT, nextX(font, x, width), y, mouseX, mouseY, page < count - 1);
	}

	private void drawArrow(GuiGraphics graphics, IComponentRenderContext context, Font font, String arrow,
			int arrowX, int arrowY, int mouseX, int mouseY, boolean enabled) {
		int color = DISABLED_ARROW;
		if (enabled) {
			color = hovered(context, font, arrow, arrowX, arrowY, mouseX, mouseY)
					? context.getHeaderColor()
					: context.getTextColor();
		}

		graphics.drawString(font, Component.literal(arrow).setStyle(context.getFont()), arrowX, arrowY, color, false);
	}

	boolean mouseClicked(IComponentRenderContext context, int x, int y, int width,
			int size, int perPage, double mouseX, double mouseY) {
		int count = pageCount(size, perPage);
		if (count <= 1) {
			return false;
		}

		Font font = Minecraft.getInstance().font;
		int mx = (int) mouseX;
		int my = (int) mouseY;

		if (page > 0 && hovered(context, font, PREVIOUS, x, y, mx, my)) {
			page--;
			playFlipSound();
			return true;
		}

		if (page < count - 1 && hovered(context, font, NEXT, nextX(font, x, width), y, mx, my)) {
			page++;
			playFlipSound();
			return true;
		}

		return false;
	}

	private boolean hovered(IComponentRenderContext context, Font font, String arrow,
			int arrowX, int arrowY, int mouseX, int mouseY) {
		return context.isAreaHovered(mouseX, mouseY, arrowX - 1, arrowY - 1,
				font.width(arrow) + 2, ARROW_HEIGHT);
	}

	private static int nextX(Font font, int x, int width) {
		return x + width - font.width(NEXT);
	}

	private static void playFlipSound() {
		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
	}
}
