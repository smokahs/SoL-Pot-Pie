package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.item.foodcontainer.FoodContainerItem;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID)
public final class FoodTracker {
	private static final Map<UUID, float[]> PRE_EAT_STATS = new ConcurrentHashMap<>();

	@SubscribeEvent
	public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

		ItemStack stack = event.getItem();
		if (!stack.isEdible()) return;

		FoodData foodData = player.getFoodData();
		PRE_EAT_STATS.put(player.getUUID(),
				new float[]{foodData.getFoodLevel(), foodData.getSaturationLevel()});
	}

	@SubscribeEvent
	public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
		if (!HeartsHandler.checkEvent(event)) {
			if (event.getEntity() instanceof Player player) {
				PRE_EAT_STATS.remove(player.getUUID());
			}
			return;
		}

		Player player = (Player) event.getEntity();

		Item usedItem = event.getItem().getItem();
		if (!usedItem.isEdible() && usedItem != Items.CAKE) return;
		if (usedItem instanceof FoodContainerItem) return;

		applyDiminishingReturns(player, event.getItem());
		updateFoodList(usedItem, player);
	}

	private static void applyDiminishingReturns(Player player, ItemStack stack) {
		Item food = stack.getItem();
		float[] preEatStats = PRE_EAT_STATS.remove(player.getUUID());

		if (!SOLPotPieConfig.diminishingReturnsEnabled()) {
			SOLPotPie.LOGGER.debug("Diminishing returns skipped for {}: disabled in the server config", food);
			return;
		}
		if (preEatStats == null) {
			SOLPotPie.LOGGER.debug("Diminishing returns skipped for {}: no pre-eat snapshot", food);
			return;
		}

		FoodList foodList = FoodList.get(player);
		int lastEaten = foodList.getLastEaten(food);
		if (lastEaten == -1) {
			SOLPotPie.LOGGER.debug("Diminishing returns skipped for {}: not in the recent queue", food);
			return;
		}

		FoodProperties properties = stack.getFoodProperties(player);
		if (properties == null) {
			SOLPotPie.LOGGER.debug("Diminishing returns skipped for {}: no food properties", food);
			return;
		}

		FoodList.MealValues meal = foodList.diminish(food, properties.getNutrition(),
				properties.getNutrition() * properties.getSaturationModifier() * 2.0F);

		FoodData foodData = player.getFoodData();
		int preFood = (int) preEatStats[0];
		float preSaturation = preEatStats[1];
		int fullFood = foodData.getFoodLevel();
		float fullSaturation = foodData.getSaturationLevel();

		int newFood = Math.min(preFood + meal.hunger(), 20);
		foodData.setFoodLevel(newFood);
		foodData.setSaturation(Math.min(preSaturation + meal.saturation(), newFood));

		SOLPotPie.LOGGER.debug(
				"Diminishing returns for {}: eaten {}x already, last eaten {} meals ago, "
						+ "meal worth {} hunger / {} saturation, hunger {}->{}, saturation {}->{}",
				food, foodList.timesEaten(food), lastEaten,
				meal.hunger(), meal.saturation(),
				fullFood, foodData.getFoodLevel(),
				fullSaturation, foodData.getSaturationLevel());

		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ClientboundSetHealthPacket(
					serverPlayer.getHealth(), foodData.getFoodLevel(), foodData.getSaturationLevel()));
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		PRE_EAT_STATS.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onCakeBlockEaten(PlayerInteractEvent.RightClickBlock event) {
		// Canceled means some other mod already resolved this event,
		// e.g. Farmer's Delight cut off a slice with a knife.
		if (event.isCanceled()) return;

		BlockState state = event.getLevel().getBlockState(event.getPos());
		Block clickedBlock = state.getBlock();
		Player player = (Player)event.getEntity();

		Item eatenItem = Items.CAKE;
		// If Farmer's Delight is installed, replace "cake" with FD's "cake slice"
		if (ModList.get().isLoaded("farmersdelight")) {
			eatenItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("farmersdelight:cake_slice"));
		}
		ItemStack eatenItemStack = new ItemStack(eatenItem);

		if (clickedBlock == Blocks.CAKE && player.canEat(false) &&
				event.getHand() == InteractionHand.MAIN_HAND && !event.getLevel().isClientSide) {
			// Fire an event instead of directly updating the food list, so that
			// SoL: Carrot Edition registers the eaten food too.
			ForgeEventFactory.onItemUseFinish(player, eatenItemStack, 0, ItemStack.EMPTY);
		}
	}

	public static void updateFoodList(Item food, Player player) {
		FoodList foodList = FoodList.get(player);
		foodList.addFood(food);

		HeartsHandler.updatePlayer(player, true);

		CapabilityHandler.syncFoodList(player);
	}

	private FoodTracker() {}
}
