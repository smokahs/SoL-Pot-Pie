package io.github.smokahs.solpotpie.tracking;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.item.foodcontainer.FoodContainerItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
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
	// snapshot of hunger/saturation at eat start, so diminishing returns can scale exactly what the meal restored
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
			return;
		}

		Player player = (Player) event.getEntity();

		Item usedItem = event.getItem().getItem();
		if (!usedItem.isEdible() && usedItem != Items.CAKE) return;
		if (usedItem instanceof FoodContainerItem) return;

		applyDiminishingReturns(player, usedItem);
		updateFoodList(usedItem, player);
	}

	/** scales down what the meal restored by recency; runs before re-adding, so "just ate this" hits the floor */
	private static void applyDiminishingReturns(Player player, Item food) {
		float[] preEatStats = PRE_EAT_STATS.remove(player.getUUID());

		if (!SOLPotPieConfig.diminishingReturnsEnabled()) return;
		if (preEatStats == null) return; // e.g. cake block bites; no snapshot to diff against

		int lastEaten = FoodList.get(player).getLastEaten(food);
		if (lastEaten == -1) return; // not eaten recently: full value

		double floor = SOLPotPieConfig.diminishingFloor();
		double freshness = Math.min(1.0, (lastEaten + 1) / (double) SOLPotPieConfig.size());
		double multiplier = floor + (1.0 - floor) * freshness;

		FoodData foodData = player.getFoodData();
		int preFood = (int) preEatStats[0];
		float preSaturation = preEatStats[1];

		int gainedFood = foodData.getFoodLevel() - preFood;
		float gainedSaturation = foodData.getSaturationLevel() - preSaturation;

		if (gainedFood > 0) {
			foodData.setFoodLevel(preFood + (int) Math.round(gainedFood * multiplier));
		}
		if (gainedSaturation > 0) {
			foodData.setSaturation(preSaturation + (float) (gainedSaturation * multiplier));
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
