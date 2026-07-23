package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.ConfigHandler;
import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID)
public final class HeartsHandler {
	private static final UUID HEART_MODIFIER_ID = UUID.fromString("2d43f5c1-6d77-45e2-a1cf-c1b9a87e2b01");
	private static final String HEART_MODIFIER_NAME = "solpotpie_hearts";

	private HeartsHandler() {}

	public static double costOfHeart(int n) {
		return SOLPotPieConfig.baseHeartCost() + SOLPotPieConfig.heartCostIncrement() * (n - 1);
	}

	public static double cumulativeCost(int n) {
		return SOLPotPieConfig.baseHeartCost() * n
				+ SOLPotPieConfig.heartCostIncrement() * n * (n - 1) / 2.0;
	}

	public static int heartsFromPoints(double points) {
		int hearts = 0;
		while (points >= cumulativeCost(hearts + 1) && hearts < 100000) {
			hearts++;
		}

		int max = SOLPotPieConfig.maxHearts();
		if (max > 0) {
			hearts = Math.min(hearts, max);
		}
		return hearts;
	}

	public static void updatePlayer(Player player) {
		updatePlayer(player, false);
	}

	public static void updatePlayer(Player player, boolean celebrate) {
		if (player.level().isClientSide) {
			return;
		}

		FoodList foodList = FoodList.get(player);
		boolean gained = applyHearts(player, heartsFromPoints(foodList.lifetimePoints()));

		if (celebrate && gained) {
			celebrate(player);
		}
	}

	private static void celebrate(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		ServerLevel level = serverPlayer.serverLevel();
		level.sendParticles(ParticleTypes.HEART,
				serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(),
				12, 0.5, 0.5, 0.5, 0.0);
		level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	private static boolean applyHearts(Player player, int hearts) {
		AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
		if (attribute == null) {
			SOLPotPie.LOGGER.warn("Player {} has no max health attribute", player.getName().getString());
			return false;
		}

		double value = hearts * SOLPotPieConfig.healthPerHeart();

		AttributeModifier existing = attribute.getModifier(HEART_MODIFIER_ID);
		double oldValue = existing == null ? 0 : existing.getAmount();
		if (existing != null && oldValue == value) {
			return false;
		}

		float oldMax = player.getMaxHealth();

		if (existing != null) {
			attribute.removeModifier(HEART_MODIFIER_ID);
		}
		if (value != 0) {
			attribute.addPermanentModifier(
					new AttributeModifier(HEART_MODIFIER_ID, HEART_MODIFIER_NAME, value, AttributeModifier.Operation.ADDITION));
		}

		if (!ConfigHandler.isFirstAid && oldMax > 0) {
			player.setHealth(player.getHealth() * player.getMaxHealth() / oldMax);
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}

		return value > oldValue;
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!checkEvent(event)) {
			return;
		}

		updatePlayer((Player) event.getEntity());
		CapabilityHandler.syncFoodList(event.getEntity());
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!checkEvent(event)) {
			return;
		}

		updatePlayer((Player) event.getEntity());
	}

	public static boolean checkEvent(LivingEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return false;

		if (player.level().isClientSide)
			return false;

		ServerPlayer serverPlayer = (ServerPlayer) player;
		boolean isInSurvival = serverPlayer.gameMode.isSurvival();
		return !SOLPotPieConfig.limitProgressionToSurvival() || isInSurvival;
	}
}
