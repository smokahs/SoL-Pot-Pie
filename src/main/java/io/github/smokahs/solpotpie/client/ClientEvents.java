package io.github.smokahs.solpotpie.client;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.Sync;
import io.github.smokahs.solpotpie.book.FoodBook;
import io.github.smokahs.solpotpie.foodgroups.FoodGroups;
import io.github.smokahs.solpotpie.item.SOLPotPieItems;
import io.github.smokahs.solpotpie.tracking.PackTotals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static io.github.smokahs.solpotpie.client.SOLClientRegistry.OPEN_FOOD_BOOK;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLPotPie.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
	@SubscribeEvent
	public static void handleKeypress(TickEvent.ClientTickEvent event) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || OPEN_FOOD_BOOK == null) {
			return;
		}

		while (OPEN_FOOD_BOOK.consumeClick()) {
			if (carriesFoodBook(player)) {
				FoodBook.openOnClient();
			}
		}
	}

	private static boolean carriesFoodBook(LocalPlayer player) {
		return player.getInventory().hasAnyMatching(stack -> stack.is(SOLPotPieItems.FOOD_BOOK.get()));
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		Sync.clear();
		PackTotals.invalidate();
		FoodGroups.clear();
	}
}
