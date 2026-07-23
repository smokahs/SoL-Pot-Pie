package io.github.smokahs.solpotpie.book;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.ICustomComponent;

import java.util.ArrayList;
import java.util.function.UnaryOperator;
import java.util.List;

import static io.github.smokahs.solpotpie.lib.Localization.localized;
import static io.github.smokahs.solpotpie.lib.Localization.spelledNumber;

@OnlyIn(Dist.CLIENT)
public class MenuComponent implements ICustomComponent {
	private static final int PANEL_OFFSET = 126;
	private static final int PANEL_WIDTH = 104;
	private static final int ICON = 16;

	private static final int BUTTON_FILL = 0x18000000;
	private static final int BUTTON_FILL_HOVERED = 0x33000000;
	private static final int BUTTON_BORDER = 0x30000000;

	private static final int SEPARATOR_U = 140;
	private static final int SEPARATOR_V = 180;
	private static final int SEPARATOR_WIDTH = 110;
	private static final int SEPARATOR_HEIGHT = 3;

	private static final int PAGE_HEIGHT = 156;
	private static final int PAGER_HEIGHT = 12;

	@SerializedName("button_width") int buttonWidth = 110;
	@SerializedName("button_height") int buttonHeight = 18;
	@SerializedName("button_spacing") int buttonSpacing = 2;
	@SerializedName("child_indent") int childIndent = 10;
	@SerializedName("max_height") int maxHeight = -1;
	@SerializedName("preview_rows") int previewRows = 2;
	@SerializedName("line_height") int lineHeight = 12;
	@SerializedName("panel_offset") int panelOffset = PANEL_OFFSET;
	@SerializedName("panel_width") int panelWidth = PANEL_WIDTH;
	List<MenuButton> buttons = new ArrayList<>();

	private transient int x;
	private transient int y;
	private transient MenuButton selected;
	private transient Pager pager;
	private transient List<BookStats.Row> statRows = new ArrayList<>();
	private transient List<ItemStack> recentFoods = new ArrayList<>();
	private transient List<ItemStack> untastedFoods = new ArrayList<>();

	@Override
	public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {}

	@Override
	public void build(int componentX, int componentY, int pageNum) {
		x = componentX;
		y = componentY;
		pager = new Pager();
		buttons.forEach(MenuButton::resolve);
	}

	private int slotHeight() {
		return buttonHeight + buttonSpacing;
	}

	private int available() {
		return maxHeight > 0 ? maxHeight : PAGE_HEIGHT - y;
	}

	private int perPage(int total) {
		int fits = Math.max(1, available() / slotHeight());
		if (total <= fits) {
			return fits;
		}
		return Math.max(1, (available() - PAGER_HEIGHT) / slotHeight());
	}

	@Override
	public void onDisplayed(IComponentRenderContext context) {
		refresh();
	}

	private void refresh() {
		statRows = BookStats.rows();
		recentFoods = BookFoods.recent();
		untastedFoods = BookFoods.untasted();
	}

	private List<Slot> layout() {
		List<Slot> slots = new ArrayList<>();
		for (MenuButton button : buttons) {
			slots.add(new Slot(button, 0));
			if (button.expanded) {
				for (MenuButton child : button.children) {
					slots.add(new Slot(child, 1));
				}
			}
		}
		return slots;
	}

	@Override
	public void render(GuiGraphics graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
		if (buttons.isEmpty()) {
			return;
		}
		if (statRows.isEmpty()) {
			refresh();
		}

		Font font = Minecraft.getInstance().font;
		List<Slot> slots = layout();
		int perPage = perPage(slots.size());
		pager.clamp(slots.size(), perPage);

		int first = pager.page() * perPage;
		int last = Math.min(first + perPage, slots.size());
		for (int i = first; i < last; i++) {
			Slot slot = slots.get(i);
			int slotX = x + slot.depth() * childIndent;
			int slotWidth = buttonWidth - slot.depth() * childIndent;
			int slotY = slotY(i - first);

			boolean hovered = context.isAreaHovered(mouseX, mouseY, slotX, slotY, slotWidth, buttonHeight);
			if (hovered) {
				selected = slot.button();
			}

			drawButton(graphics, context, font, slot.button(), slotX, slotY, slotWidth,
					hovered || slot.button() == selected);
		}

		pager.render(graphics, context, x, slotY(perPage), buttonWidth,
				slots.size(), perPage, mouseX, mouseY);

		if (selected == null) {
			selected = buttons.get(0);
		}
		drawPanel(graphics, context, font, selected, mouseX, mouseY);
	}

	private void drawButton(GuiGraphics graphics, IComponentRenderContext context, Font font,
			MenuButton button, int buttonX, int buttonY, int width, boolean highlighted) {
		int right = buttonX + width;
		int bottom = buttonY + buttonHeight;

		graphics.fill(buttonX, buttonY, right, bottom, highlighted ? BUTTON_FILL_HOVERED : BUTTON_FILL);

		graphics.fill(buttonX, buttonY, right, buttonY + 1, BUTTON_BORDER);
		graphics.fill(buttonX, bottom - 1, right, bottom, BUTTON_BORDER);
		graphics.fill(buttonX, buttonY, buttonX + 1, bottom, BUTTON_BORDER);
		graphics.fill(right - 1, buttonY, right, bottom, BUTTON_BORDER);
		if (highlighted) {
			graphics.fill(buttonX, buttonY, buttonX + 2, bottom, BookStats.opaque(context.getHeaderColor()));
		}

		if (!button.iconStack.isEmpty()) {
			graphics.renderItem(button.iconStack, buttonX + 5, buttonY + (buttonHeight - ICON) / 2);
		}

		int color = highlighted ? context.getHeaderColor() : context.getTextColor();
		int textY = buttonY + (buttonHeight - font.lineHeight) / 2 + 1;
		graphics.drawString(font,
				Component.literal(button.labelText()).setStyle(context.getFont().withBold(highlighted)),
				buttonX + 5 + ICON + 4, textY, color, false);

		if (!button.children.isEmpty()) {
			String marker = button.expanded ? "-" : "+";
			graphics.drawString(font, Component.literal(marker).setStyle(context.getFont()),
					right - 4 - font.width(marker), textY, color, false);
		}
	}

	private void drawPanel(GuiGraphics graphics, IComponentRenderContext context, Font font,
			MenuButton button, int mouseX, int mouseY) {
		int panelX = x + panelOffset;
		int panelY = y;

		String title = button.labelText();
		graphics.drawString(font, Component.literal(title).setStyle(context.getFont().withBold(true)),
				panelX + (panelWidth - font.width(title)) / 2, panelY, context.getHeaderColor(), false);
		panelY += font.lineHeight + 3;

		drawSeparator(graphics, context, panelX, panelY);
		panelY += SEPARATOR_HEIGHT + 5;

		switch (button.panel) {
			case "stats" -> BookStats.drawRows(graphics, context, statRows,
					panelX, panelY, panelWidth, lineHeight, mouseX, mouseY);
			case "recent" -> drawFoodPreview(graphics, context, font, panelX, panelY, mouseX, mouseY,
					recentFoods, "food_book.queue.empty", "food_book.menu.recent_summary");
			case "untasted" -> drawFoodPreview(graphics, context, font, panelX, panelY, mouseX, mouseY,
					untastedFoods, "food_book.untasted.empty", "food_book.menu.untasted_summary");
			default -> drawText(graphics, context, font, button, panelX, panelY);
		}
	}

	private void drawSeparator(GuiGraphics graphics, IComponentRenderContext context, int sepX, int sepY) {
		RenderSystem.enableBlend();
		graphics.setColor(1F, 1F, 1F, 0.8F);
		graphics.blit(context.getBookTexture(), sepX + (panelWidth - SEPARATOR_WIDTH) / 2, sepY,
				SEPARATOR_U, SEPARATOR_V, SEPARATOR_WIDTH, SEPARATOR_HEIGHT, 512, 256);
		graphics.setColor(1F, 1F, 1F, 1F);
	}

	private void drawFoodPreview(GuiGraphics graphics, IComponentRenderContext context, Font font,
			int panelX, int panelY, int mouseX, int mouseY,
			List<ItemStack> foods, String emptyKey, String summaryKey) {
		if (foods.isEmpty()) {
			graphics.drawString(font,
					Component.literal(localized("gui", emptyKey)).setStyle(context.getFont()),
					panelX, panelY, context.getHeaderColor(), false);
			return;
		}

		int columns = panelWidth / FoodGrid.SLOT;
		int shown = Math.min(foods.size(), columns * previewRows);
		for (int i = 0; i < shown; i++) {
			context.renderItemStack(graphics,
					panelX + (i % columns) * FoodGrid.SLOT, panelY + (i / columns) * FoodGrid.SLOT,
					mouseX, mouseY, foods.get(i));
		}

		int rows = (shown + columns - 1) / columns;
		String summary = shown == 1
				? localized("gui", summaryKey + ".singular")
				: localized("gui", summaryKey + ".plural", spelledNumber(shown));
		drawWrapped(graphics, context, font, summary, panelX, panelY + rows * FoodGrid.SLOT + 4);
	}

	private void drawText(GuiGraphics graphics, IComponentRenderContext context, Font font,
			MenuButton button, int panelX, int panelY) {
		if (button.text == null) {
			return;
		}

		drawWrapped(graphics, context, font, localized("gui", button.text), panelX, panelY);
	}

	private void drawWrapped(GuiGraphics graphics, IComponentRenderContext context, Font font,
			String text, int textX, int textY) {
		List<FormattedCharSequence> wrapped =
				font.split(Component.literal(text).setStyle(context.getFont()), panelWidth);
		for (FormattedCharSequence line : wrapped) {
			graphics.drawString(font, line, textX, textY, context.getTextColor(), false);
			textY += font.lineHeight;
		}
	}

	@Override
	public boolean mouseClicked(IComponentRenderContext context, double mouseX, double mouseY, int mouseButton) {
		int mx = (int) mouseX;
		int my = (int) mouseY;
		List<Slot> slots = layout();
		int perPage = perPage(slots.size());

		if (pager.mouseClicked(context, x, slotY(perPage), buttonWidth,
				slots.size(), perPage, mouseX, mouseY)) {
			return true;
		}

		int first = pager.page() * perPage;
		int last = Math.min(first + perPage, slots.size());
		for (int i = first; i < last; i++) {
			Slot slot = slots.get(i);
			int slotX = x + slot.depth() * childIndent;
			int slotWidth = buttonWidth - slot.depth() * childIndent;
			if (!context.isAreaHovered(mx, my, slotX, slotY(i - first), slotWidth, buttonHeight)) {
				continue;
			}

			MenuButton button = slot.button();
			if (!button.children.isEmpty()) {
				button.expanded = !button.expanded;
				playFlipSound();
				return true;
			}

			if (button.entryId != null && context.navigateToEntry(button.entryId, 0, true)) {
				playFlipSound();
				return true;
			}
		}

		return false;
	}

	private int slotY(int index) {
		return y + index * slotHeight();
	}

	private static void playFlipSound() {
		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
	}

	private record Slot(MenuButton button, int depth) {}

	static class MenuButton {
		String icon;
		String label;
		String entry;
		String panel = "text";
		String text;
		List<MenuButton> children = new ArrayList<>();

		transient ItemStack iconStack = ItemStack.EMPTY;
		transient ResourceLocation entryId;
		transient boolean expanded;

		void resolve() {
			if (icon != null) {
				Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(icon));
				iconStack = item == null ? ItemStack.EMPTY : new ItemStack(item);
			}
			if (entry != null) {
				entryId = new ResourceLocation(entry);
			}
			if (children == null) {
				children = new ArrayList<>();
			}
			children.forEach(MenuButton::resolve);
		}

		String labelText() {
			return label == null ? "" : localized("gui", label);
		}
	}
}
