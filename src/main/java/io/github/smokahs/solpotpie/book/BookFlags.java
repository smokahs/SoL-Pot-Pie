package io.github.smokahs.solpotpie.book;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.ClientBookRegistry;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLPotPie.MOD_ID, bus = MOD)
public final class BookFlags {
	public static final String SHOW_FOOD_SCORES = "solpotpie:show_food_scores";

	private static Boolean lastShowScores = null;

	private BookFlags() {}

	@SubscribeEvent
	public static void onConfigLoad(ModConfigEvent.Loading event) {
		refresh(event.getConfig());
	}

	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event) {
		refresh(event.getConfig());
	}

	private static void refresh(ModConfig config) {
		if (config.getType() != ModConfig.Type.CLIENT) return;

		boolean showScores = SOLPotPieConfig.isFoodTooltipEnabled()
				&& !SOLPotPieConfig.shouldHideValuesUntilEaten();
		if (lastShowScores != null && lastShowScores == showScores) return;

		lastShowScores = showScores;
		PatchouliAPI.get().setConfigFlag(SHOW_FOOD_SCORES, showScores);

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.level != null) {
			ClientBookRegistry.INSTANCE.reload();
		}
	}
}
