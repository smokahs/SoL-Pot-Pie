package io.github.smokahs.solpotpie.book;

import io.github.smokahs.solpotpie.SOLPotPie;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.patchouli.api.PatchouliAPI;

public final class FoodBook {
	public static final ResourceLocation ID = SOLPotPie.resourceLocation("food_book");

	public static final ResourceLocation MENU_ENTRY = SOLPotPie.resourceLocation("diet/menu");

	private FoodBook() {}

	public static void open(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			PatchouliAPI.get().openBookEntry(serverPlayer, ID, MENU_ENTRY, 0);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void openOnClient() {
		try {
			PatchouliAPI.get().openBookEntry(ID, MENU_ENTRY, 0);
		} catch (RuntimeException e) {
			SOLPotPie.LOGGER.warn("Could not open the food book on {}, falling back to its landing page", MENU_ENTRY, e);
			PatchouliAPI.get().openBookGUI(ID);
		}
	}
}
