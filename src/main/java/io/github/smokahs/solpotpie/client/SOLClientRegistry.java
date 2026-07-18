package io.github.smokahs.solpotpie.client;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.lib.Localization;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.platform.InputConstants;


@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SOLClientRegistry {
	public static KeyMapping OPEN_FOOD_BOOK;

	@SubscribeEvent
	public static void registerKeybinds(RegisterKeyMappingsEvent event) {
		OPEN_FOOD_BOOK = new KeyMapping(Localization.localized("key", "open_food_book"),
				InputConstants.UNKNOWN.getValue(), Localization.localized("key", "category"));
		event.register(OPEN_FOOD_BOOK);
	}
}
