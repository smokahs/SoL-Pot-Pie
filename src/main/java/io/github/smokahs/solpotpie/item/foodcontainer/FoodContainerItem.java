package io.github.smokahs.solpotpie.item.foodcontainer;

import io.github.smokahs.solpotpie.integration.Origins;
import io.github.smokahs.solpotpie.tracking.FoodList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class FoodContainerItem extends Item {
	private static final String TAG_OPEN = "open";

	private final String displayName;
	private final Supplier<Integer> slotCount;

	public FoodContainerItem(Supplier<Integer> slotCount, String displayName) {
		super(new Properties().stacksTo(1).setNoRepair());

		this.displayName = displayName;
		this.slotCount = slotCount;
	}

	@Override
	public boolean isEdible() {
		return true;
	}

	@Override
	public FoodProperties getFoodProperties() {
		return new FoodProperties.Builder().build();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isCrouching()) {
			if (!world.isClientSide) {
				boolean nowOpen = !isOpen(stack);
				setOpen(stack, nowOpen);
				world.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER,
						SoundSource.PLAYERS, 0.7F, nowOpen ? 1.2F : 0.8F);
			}
			return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
		}

		if (isOpen(stack)) {
			return processRightClick(world, player, hand);
		}

		if (!world.isClientSide) {
			NetworkHooks.openScreen((ServerPlayer) player, new FoodContainerProvider(displayName), player.blockPosition());
			setOpen(stack, true);
		}
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		if (!isOpen(stack)) {
			return InteractionResult.PASS;
		}

		Level world = context.getLevel();
		Player player = context.getPlayer();
		BlockEntity blockEntity = world.getBlockEntity(context.getClickedPos());
		if (player == null || blockEntity == null) {
			return InteractionResult.PASS;
		}

		IItemHandler target = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, context.getClickedFace())
				.resolve().orElse(null);
		if (target == null) {
			return InteractionResult.PASS;
		}

		if (!world.isClientSide) {
			ItemStackHandler container = getInventory(stack);
			if (container != null) {
				boolean moved = dumpFoodInto(container, target);
				moved |= pullBestFoodFrom(container, target, player);
				if (moved) {
					world.playSound(null, context.getClickedPos(), SoundEvents.BUNDLE_INSERT,
							SoundSource.PLAYERS, 0.8F, 1.0F);
				}
			}
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	private static boolean dumpFoodInto(ItemStackHandler container, IItemHandler target) {
		boolean moved = false;
		for (int i = 0; i < container.getSlots(); i++) {
			ItemStack stackInSlot = container.getStackInSlot(i);
			if (stackInSlot.isEmpty()) {
				continue;
			}
			ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stackInSlot, false);
			if (remainder.getCount() != stackInSlot.getCount()) {
				container.setStackInSlot(i, remainder);
				moved = true;
			}
		}
		return moved;
	}

	private record PullCandidate(int slot, double freshness, double rank) {}

	private static boolean pullBestFoodFrom(ItemStackHandler container, IItemHandler target, Player player) {
		FoodList foodList = FoodList.get(player);
		List<PullCandidate> candidates = new ArrayList<>();
		for (int i = 0; i < target.getSlots(); i++) {
			ItemStack chestStack = target.getStackInSlot(i);
			if (!chestStack.isEdible()) {
				continue;
			}
			double freshness = freshness(chestStack, player, foodList);
			if (freshness <= 0) {
				continue;
			}
			candidates.add(new PullCandidate(i, freshness, foodList.rankFood(chestStack.getItem())));
		}
		candidates.sort(Comparator.comparingDouble(PullCandidate::freshness)
				.thenComparingDouble(PullCandidate::rank).reversed());

		boolean moved = false;
		for (PullCandidate candidate : candidates) {
			int slotNum = candidate.slot();
			ItemStack available = target.extractItem(slotNum, target.getStackInSlot(slotNum).getMaxStackSize(), true);
			if (available.isEmpty() || !available.isEdible()) {
				continue;
			}
			int accepted = insertOnce(container, available, true);
			if (accepted <= 0) {
				continue;
			}
			ItemStack extracted = target.extractItem(slotNum, accepted, false);
			if (extracted.isEmpty()) {
				continue;
			}
			int inserted = insertOnce(container, extracted, false);
			if (inserted < extracted.getCount()) {
				ItemStack leftover = extracted.copyWithCount(extracted.getCount() - inserted);
				if (!player.getInventory().add(leftover)) {
					player.drop(leftover, false);
				}
			}
			moved = true;
		}
		return moved;
	}

	private static int insertOnce(ItemStackHandler container, ItemStack stack, boolean simulate) {
		int slot = -1;
		for (int i = 0; i < container.getSlots(); i++) {
			ItemStack existing = container.getStackInSlot(i);
			if (!existing.isEmpty() && ItemHandlerHelper.canItemStacksStack(existing, stack)
					&& existing.getCount() < Math.min(existing.getMaxStackSize(), container.getSlotLimit(i))) {
				slot = i;
				break;
			}
		}
		if (slot < 0) {
			for (int i = 0; i < container.getSlots(); i++) {
				if (container.getStackInSlot(i).isEmpty() && container.isItemValid(i, stack)) {
					slot = i;
					break;
				}
			}
		}
		if (slot < 0) {
			return 0;
		}
		ItemStack remainder = container.insertItem(slot, stack, simulate);
		return stack.getCount() - remainder.getCount();
	}

	public static boolean isOpen(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean(TAG_OPEN);
	}

	public static void setOpen(ItemStack stack, boolean open) {
		stack.getOrCreateTag().putBoolean(TAG_OPEN, open);
	}

	public static boolean hasFood(ItemStack stack) {
		return !isInventoryEmpty(stack);
	}

	private InteractionResultHolder<ItemStack> processRightClick(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ItemStackHandler handler = getInventory(stack);
		if (handler == null || getBestFoodSlot(handler, player) < 0 ||
				(ModList.get().isLoaded("origins") && Origins.hasRestrictedDiet(player))) {
			return InteractionResultHolder.pass(stack);
		}

		if (player.canEat(false)) {
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		return InteractionResultHolder.fail(stack);
	}

	private static boolean isInventoryEmpty(ItemStack container) {
		ItemStackHandler handler = getInventory(container);
		if (handler == null) {
			return true;
		}

		for (int i = 0; i < handler.getSlots(); i++) {
			ItemStack stack = handler.getStackInSlot(i);
			if (!stack.isEmpty() && stack.isEdible()) {
				return false;
			}
		}
		return true;
	}

	@Nullable
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new FoodContainerCapabilityProvider(stack, slotCount.get());
	}

	@Nullable
	public static ItemStackHandler getInventory(ItemStack bag) {
		if (bag.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent())
			return (ItemStackHandler) bag.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
		return null;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
		if (!(entity instanceof Player)) {
			return stack;
		}

		Player player = (Player) entity;
		ItemStackHandler handler = getInventory(stack);
		if (handler == null) {
			return stack;
		}

		int bestFoodSlot = getBestFoodSlot(handler, player);
		if (bestFoodSlot < 0) {
			return stack;
		}

		ItemStack bestFood = handler.getStackInSlot(bestFoodSlot);
		ItemStack foodCopy = bestFood.copy();
		if (bestFood.isEdible() && !bestFood.isEmpty()) {
			ItemStack result = bestFood.finishUsingItem(world, entity);
			if (!result.isEdible()) {
				handler.setStackInSlot(bestFoodSlot, ItemStack.EMPTY);
				Player playerEntity = (Player) entity;

				if (!playerEntity.getInventory().add(result)) {
					playerEntity.drop(result, false);
				}
			}

			if (!world.isClientSide) {
				ForgeEventFactory.onItemUseFinish(player, foodCopy, 0, result);
			}
		}

		return stack;
	}

	@Override
	public int getUseDuration(ItemStack stack){
		return 32;
	}

	public static int getBestFoodSlot(ItemStackHandler handler, Player player) {
		FoodList foodList = FoodList.get(player);

		double bestRank = -Double.MAX_VALUE;
		int bestFoodSlot = -1;
		for (int i = 0; i < handler.getSlots(); i++) {
			ItemStack food = handler.getStackInSlot(i);

			if (!food.isEdible() || food.isEmpty() || isWorthless(food, player, foodList))
				continue;
			double rank = foodList.rankFood(food.getItem());
			if (rank > bestRank) {
				bestRank = rank;
				bestFoodSlot = i;
			}
		}

		return bestFoodSlot;
	}

	// mirrors the eat-time guard direct eating gets: never feed a food diminished to nothing
	private static boolean isWorthless(ItemStack food, Player player, FoodList foodList) {
		return freshness(food, player, foodList) <= 0;
	}

	// fraction of full hunger+saturation the food currently restores, 0..1
	private static double freshness(ItemStack food, Player player, FoodList foodList) {
		FoodProperties properties = food.getFoodProperties(player);
		if (properties == null) return 0;

		float fullSaturation = properties.getNutrition() * properties.getSaturationModifier() * 2.0F;
		double full = properties.getNutrition() + fullSaturation;
		if (full <= 0) return 0;

		FoodList.MealValues meal = foodList.diminish(food.getItem(), properties.getNutrition(), fullSaturation);
		return (meal.hunger() + meal.saturation()) / full;
	}
}
