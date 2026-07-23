package io.github.smokahs.solpotpie.book;

import io.github.smokahs.solpotpie.tracking.FoodList;
import io.github.smokahs.solpotpie.tracking.HeartsHandler;
import io.github.smokahs.solpotpie.tracking.PackTotals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.patchouli.api.IComponentRenderContext;

import java.util.ArrayList;
import java.util.List;

import static io.github.smokahs.solpotpie.lib.Localization.localized;

@OnlyIn(Dist.CLIENT)
public final class BookStats {
	private BookStats() {}

	private static final float NO_BAR = -1F;

	public static final int BAR_HEART = 0xB0392B;
	public static final int BAR_TASTED = 0x4F8A3C;

	public record Row(String label, String value, String tooltip, String shiftTooltip,
			float progress, int barColor) {
		public Row(String label, String value, String tooltip) {
			this(label, value, tooltip, null, NO_BAR, 0);
		}

		public Row(String label, String value, String tooltip, String shiftTooltip) {
			this(label, value, tooltip, shiftTooltip, NO_BAR, 0);
		}

		public Row withBar(float progress, int color) {
			return new Row(label, value, tooltip, shiftTooltip, progress, color);
		}

		boolean hasBar() {
			return progress >= 0F;
		}
	}

	public static List<Row> rows() {
		List<Row> rows = new ArrayList<>();

		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return rows;
		}

		FoodList foodList = FoodList.get(player);
		double lifetimePoints = foodList.lifetimePoints();
		int hearts = HeartsHandler.heartsFromPoints(lifetimePoints);
		double nextHeartAt = HeartsHandler.cumulativeCost(hearts + 1);

		int packFoods = PackTotals.foodCount();
		int packHearts = PackTotals.maxHearts();
		boolean hasTotals = packFoods > 0;

		rows.add(new Row(
				localized("gui", "food_book.stats.hearts"),
				hasTotals ? fraction(hearts, packHearts) : Integer.toString(hearts),
				hasTotals ? localized("gui", "food_book.stats.hearts_tooltip", packHearts) : null));

		rows.add(new Row(
				localized("gui", "food_book.stats.lifetime_points"),
				hasTotals
						? fraction(points(lifetimePoints), PackTotals.maxPointsText())
						: points(lifetimePoints),
				hasTotals
						? localized("gui", "food_book.stats.lifetime_points_tooltip", PackTotals.maxPointsText())
						: null,
				hasTotals ? localized("gui", "food_book.stats.lifetime_points_warning") : null));

		boolean maxedOut = hasTotals && hearts >= packHearts;
		Row nextHeart = new Row(
				maxedOut
						? localized("gui", "food_book.stats.next_heart_maxed")
						: localized("gui", "food_book.stats.next_heart", points(nextHeartAt - lifetimePoints)),
				"",
				maxedOut
						? localized("gui", "food_book.stats.next_heart_maxed_tooltip")
						: localized("gui", "food_book.stats.next_heart_tooltip"));
		double heartFloor = HeartsHandler.cumulativeCost(hearts);
		double heartSpan = nextHeartAt - heartFloor;
		rows.add(nextHeart.withBar(
				maxedOut || heartSpan <= 0 ? 1F : (float) ((lifetimePoints - heartFloor) / heartSpan),
				BAR_HEART));

		if (hasTotals) {
			rows.add(new Row(
					localized("gui", "food_book.stats.foods_tasted_label"),
					fraction(foodList.discoveredFoods(), packFoods),
					localized("gui", "food_book.stats.foods_tasted_tooltip", packFoods))
					.withBar((float) foodList.discoveredFoods() / packFoods, BAR_TASTED));
		}

		return rows;
	}

	private static final int VALUE_GAP = 6;

	public static int drawRows(GuiGraphics graphics, IComponentRenderContext context, List<Row> rows,
			int x, int y, int width, int lineHeight, int mouseX, int mouseY) {
		Font font = Minecraft.getInstance().font;
		int rowY = y;

		for (Row row : rows) {
			int valueWidth = row.value().isEmpty() ? 0 : font.width(row.value());
			boolean wrapped = valueWidth > 0 && font.width(row.label()) + VALUE_GAP + valueWidth > width;
			int rowHeight = wrapped ? lineHeight * 2 : lineHeight;

			Style labelStyle = valueWidth == 0 ? context.getFont().withItalic(true) : context.getFont();
			graphics.drawString(font, Component.literal(row.label()).setStyle(labelStyle),
					x, rowY, context.getTextColor(), false);

			if (valueWidth > 0) {
				graphics.drawString(font,
						Component.literal(row.value()).setStyle(context.getFont().withBold(true)),
						wrapped ? x + (width - valueWidth) / 2 : x + width - valueWidth,
						wrapped ? rowY + lineHeight : rowY,
						context.getHeaderColor(), false);
			}

			if (row.tooltip() != null && context.isAreaHovered(mouseX, mouseY, x, rowY - 1, width, rowHeight)) {
				context.setHoverTooltipComponents(tooltipFor(row));
			}

			rowY += rowHeight;

			if (row.hasBar()) {
				drawBar(graphics, x, rowY + (lineHeight - BAR_HEIGHT) / 2, width,
						row.progress(), row.barColor());
				rowY += lineHeight;
			}
		}

		return rowY - y;
	}

	private static final int BAR_HEIGHT = 3;
	private static final int BAR_BACKGROUND = 0x30000000;

	private static void drawBar(GuiGraphics graphics, int barX, int barY, int width,
			float progress, int color) {
		graphics.fill(barX, barY, barX + width, barY + BAR_HEIGHT, BAR_BACKGROUND);

		int filled = Math.round(width * Mth.clamp(progress, 0F, 1F));
		if (filled > 0) {
			graphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, opaque(color));
		}
	}

	static int opaque(int color) {
		return color | 0xFF000000;
	}

	private static String fraction(Object numerator, Object denominator) {
		return localized("gui", "food_book.fraction", numerator, denominator);
	}

	private static String points(double value) {
		return String.format("%.1f", value);
	}

	public static List<Component> tooltipFor(Row row) {
		List<Component> components = lines(row.tooltip());
		if (row.shiftTooltip() == null) {
			return components;
		}

		if (Screen.hasShiftDown()) {
			components.addAll(lines(row.shiftTooltip()));
		} else {
			components.add(Component.literal(localized("gui", "food_book.stats.hold_shift")));
		}
		return components;
	}

	private static final int TOOLTIP_WIDTH = 170;

	public static List<Component> lines(String text) {
		return lines(text, TOOLTIP_WIDTH);
	}

	public static List<Component> lines(String text, int maxWidth) {
		Font font = Minecraft.getInstance().font;
		List<Component> components = new ArrayList<>();

		for (String paragraph : text.split("\n")) {
			String carried = "";
			StringBuilder line = new StringBuilder();

			for (String word : paragraph.split(" ")) {
				if (line.length() > 0 && font.width(carried + line + " " + word) > maxWidth) {
					components.add(Component.literal(carried + line));
					carried = trailingFormat(carried, line.toString());
					line = new StringBuilder(word);
				} else {
					line.append(line.length() > 0 ? " " : "").append(word);
				}
			}

			components.add(Component.literal(carried + line));
		}

		return components;
	}

	private static String trailingFormat(String carried, String line) {
		String active = carried;
		for (int i = 0; i < line.length() - 1; i++) {
			if (line.charAt(i) == '\u00a7') {
				char code = line.charAt(i + 1);
				active = code == 'r' ? "" : "\u00a7" + code;
			}
		}
		return active;
	}
}
