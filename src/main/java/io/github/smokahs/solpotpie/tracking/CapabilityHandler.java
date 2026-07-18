package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.api.FoodCapability;
import io.github.smokahs.solpotpie.communication.FoodListMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID)
public final class CapabilityHandler {
	private static final ResourceLocation FOOD = SOLPotPie.resourceLocation("food");

	@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID, bus = MOD)
	private static final class RegisterCapabilitiesSubscriber {
		@SubscribeEvent
		public static void registerCapabilities(RegisterCapabilitiesEvent event) {
			event.register(FoodCapability.class);
		}
	}

	@SubscribeEvent
	public static void attachPlayerCapability(AttachCapabilitiesEvent<Entity> event) {
		if (!(event.getObject() instanceof Player)) return;

		event.addCapability(FOOD, new FoodList());
	}

	@SubscribeEvent
	public static void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		syncFoodList(event.getEntity());
	}

	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		Player originalPlayer = event.getOriginal();
		originalPlayer.revive(); // so we can access the capabilities; entity will get removed either way
		FoodList original = FoodList.get(originalPlayer);
		FoodList newInstance = FoodList.get(event.getEntity());
		newInstance.deserializeNBT(original.serializeNBT());

		// hearts are permanent; death only optionally resets the recent queue
		if (event.isWasDeath() && SOLPotPieConfig.shouldResetOnDeath()) {
			newInstance.clearRecent();
		}
		// can't sync yet; client hasn't attached capabilities yet

		HeartsHandler.updatePlayer(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		syncFoodList(event.getEntity());
	}

	public static void syncFoodList(Player player) {
		if (player.level().isClientSide) return;

		ServerPlayer target = (ServerPlayer) player;
		SOLPotPie.channel.sendTo(
			new FoodListMessage(FoodList.get(player)),
			target.connection.connection,
			NetworkDirection.PLAY_TO_CLIENT
		);
	}
}
