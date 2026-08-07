package io.github.smokahs.solpotpie.integration;

import com.mojang.datafixers.util.Either;
import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.tracking.FoodList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import sfiomn.legendarysurvivaloverhaul.client.tooltips.HydrationTooltipComponent;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLPotPie.MOD_ID)
public final class ModTooltipSupport {
	private static final boolean HAS_LSO = ModList.get().isLoaded("legendarysurvivaloverhaul");
	private static final List<String> KEY_PREFIXES = buildKeyPrefixes();

	// equipment header lso rewrites hand modifier sections into, not food info, so it stays
	private static final String LSO_HAND_HEADER = "tooltip.legendarysurvivaloverhaul.modifiers.hand";

	private static List<String> buildKeyPrefixes() {
		ModList mods = ModList.get();
		List<String> prefixes = new ArrayList<>();
		if (mods.isLoaded("diet")) {
			prefixes.add("tooltip.diet.");
		}
		if (mods.isLoaded("farmersdelight")) {
			prefixes.add("tooltip.farmersdelight.");
		}
		if (HAS_LSO) {
			prefixes.add("tooltip.legendarysurvivaloverhaul.");
		}
		if (mods.isLoaded("foodeffecttooltips")) {
			prefixes.add("foodeffecttooltips.");
		}
		if (mods.isLoaded("hungeroverhauled")) {
			prefixes.add("hungeroverhauled.tooltip.");
		}
		if (mods.isLoaded("farmersdelight") || mods.isLoaded("foodeffecttooltips") || HAS_LSO) {
			prefixes.add("effect.");
			prefixes.add("potion.with");
			prefixes.add("potion.whenDrank");
		}
		return prefixes;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (KEY_PREFIXES.isEmpty()) return;
		if (!shouldFold(event.getEntity(), event.getItemStack().getItem())) return;

		// a stripped line drags its continuation lines with it: attribute modifiers and
		// muted literal lines other mods print under a translatable header. a blank
		// separator line is only kept if the section it introduces survives
		List<Component> source = event.getToolTip();
		List<Component> kept = new ArrayList<>(source.size());
		Component pendingBlank = null;
		boolean stripping = false;
		boolean removedAny = false;
		for (Component line : source) {
			if (line.getString().isBlank()) {
				if (pendingBlank != null) {
					kept.add(pendingBlank);
				}
				pendingBlank = line;
				stripping = false;
				continue;
			}
			if (isForeignLine(line)) {
				pendingBlank = null;
				stripping = true;
				removedAny = true;
				continue;
			}
			if (stripping && isContinuationLine(line)) {
				removedAny = true;
				continue;
			}
			stripping = false;
			if (pendingBlank != null) {
				kept.add(pendingBlank);
				pendingBlank = null;
			}
			kept.add(line);
		}
		if (pendingBlank != null) {
			kept.add(pendingBlank);
		}

		if (removedAny) {
			source.clear();
			source.addAll(kept);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
		if (!HAS_LSO) return;
		if (!shouldFold(Minecraft.getInstance().player, event.getItemStack().getItem())) return;

		Lso.stripHydration(event.getTooltipElements());
	}

	private static boolean shouldFold(Player player, Item food) {
		if (!SOLPotPieConfig.modTooltipSupportEnabled()) return false;
		if (!SOLPotPieConfig.isFoodTooltipEnabled()) return false;
		if (player == null) return false;
		if (!food.isEdible()) return false;
		if (!SOLPotPieConfig.hasTooltip(food)) return false;
		if (!SOLPotPieConfig.isAllowed(food)) return false;

		if (SOLPotPieConfig.shouldHideValuesUntilEaten() && !FoodList.get(player).hasEverEaten(food)) {
			return true;
		}
		return !Screen.hasShiftDown();
	}

	private static boolean isForeignLine(Component component) {
		if (component.getContents() instanceof TranslatableContents translatable) {
			if (matchesAnyPrefix(translatable.getKey())) {
				return true;
			}
			Object[] args = translatable.getArgs();
			if (args != null) {
				for (Object arg : args) {
					if (arg instanceof Component inner && isForeignLine(inner)) {
						return true;
					}
				}
			}
		}
		for (Component sibling : component.getSiblings()) {
			if (isForeignLine(sibling)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesAnyPrefix(String key) {
		if (key.equals(LSO_HAND_HEADER)) {
			return false;
		}
		for (String prefix : KEY_PREFIXES) {
			if (key.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isContinuationLine(Component line) {
		if (line.getContents() instanceof TranslatableContents translatable) {
			return translatable.getKey().startsWith("attribute.modifier.");
		}
		if (!(line.getContents() instanceof LiteralContents)) {
			return false;
		}
		if (line.getString().isBlank()) {
			return false;
		}
		Style style = line.getStyle();
		if (style.isItalic()) {
			return false;
		}
		TextColor color = style.getColor();
		return color == null
				|| color.equals(TextColor.fromLegacyFormat(ChatFormatting.GRAY))
				|| color.equals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY));
	}

	private static final class Lso {
		static void stripHydration(List<Either<FormattedText, TooltipComponent>> elements) {
			elements.removeIf(element ->
					element.right().map(part -> part instanceof HydrationTooltipComponent).orElse(false));
		}

		private Lso() {}
	}

	private ModTooltipSupport() {}
}
