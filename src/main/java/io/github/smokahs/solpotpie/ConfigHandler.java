package io.github.smokahs.solpotpie;

import io.github.smokahs.solpotpie.communication.ConfigMessage;
import io.github.smokahs.solpotpie.foodgroups.FoodGroups;
import io.github.smokahs.solpotpie.tracking.FoodInstance;
import io.github.smokahs.solpotpie.tracking.FoodScores;
import io.github.smokahs.solpotpie.tracking.PackTotals;
import io.github.smokahs.solpotpie.utils.ComplexityParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.*;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID)
public class ConfigHandler {
	public static Map<FoodInstance, Double> scoreMap = new HashMap<>();

	public final static String SCORE_MAP_KEY = "score_map";
	public final static String SETTINGS_KEY = "settings";
	public final static String FOOD_GROUPS_KEY = "food_groups";
	public final static String FOOD_KEY = "food";
	public final static String SCORE_VALUE_KEY = "score";
	public final static String ENTRY_KEY = "entries";

	public static boolean isFirstAid = false;

	public static CompoundTag serializeScoreMap() {
		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		for (Map.Entry<FoodInstance, Double> entry : scoreMap.entrySet()) {
			CompoundTag entryTag = new CompoundTag();
			String encoded = entry.getKey().encode();
			entryTag.put(FOOD_KEY, StringTag.valueOf(encoded));
			entryTag.put(SCORE_VALUE_KEY, DoubleTag.valueOf(entry.getValue()));
			list.add(entryTag);
		}
		tag.put(ENTRY_KEY, list);
		return tag;
	}

	public static CompoundTag serializeConfig() {
		CompoundTag tag = new CompoundTag();
		tag.put(SCORE_MAP_KEY, serializeScoreMap());
		tag.put(SETTINGS_KEY, Sync.serialize());
		tag.put(FOOD_GROUPS_KEY, FoodGroups.serialize());
		return tag;
	}

	public static void deserializeConfig(CompoundTag tag) {
		Sync.accept(tag.getCompound(SETTINGS_KEY));
		deserializeScoreMap(tag.getCompound(SCORE_MAP_KEY));
		FoodGroups.deserialize(tag.getCompound(FOOD_GROUPS_KEY));
	}

	public static void deserializeScoreMap(CompoundTag tag) {
		ListTag list = tag.getList(ENTRY_KEY, Tag.TAG_COMPOUND);
		Map<FoodInstance, Double> newScoreMap = new HashMap<>();
		for (Tag nbt : list) {
			CompoundTag cnbt = (CompoundTag) nbt;
			String foodString = cnbt.getString(FOOD_KEY);
			FoodInstance food = FoodInstance.decode(foodString);
			double score = cnbt.getDouble(SCORE_VALUE_KEY);
			newScoreMap.put(food, score);
		}
		scoreMap = newScoreMap;
		PackTotals.invalidate();
	}

	public static void rebuildScores(MinecraftServer server) {
		Map<FoodInstance, Double> newScoreMap = FoodScores.compute();
		newScoreMap.putAll(ComplexityParser.parse(SOLPotPieConfig.getScoreOverrides()));
		scoreMap = newScoreMap;
		PackTotals.invalidate();

		SOLPotPie.LOGGER.info("Pack ceiling: {} tracked foods, {} points, {} hearts",
				PackTotals.foodCount(), PackTotals.maxPointsText(), PackTotals.maxHearts());
	}

	@SubscribeEvent
	public static void onServerStart(ServerStartingEvent event) {
		rebuildScores(event.getServer());

		isFirstAid = ModList.get().isLoaded("firstaid");
	}

	@SubscribeEvent
	public static void onTagsUpdated(TagsUpdatedEvent event) {
		// the server reads the folder. fires on world load and again on every /reload.
		if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
			FoodGroups.reload();
			resyncFoodGroups();
			return;
		}

		FoodGroups.resolve();
	}

	private static void resyncFoodGroups() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			syncConfig(player);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		FoodGroups.clear();
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		syncConfig(event.getEntity());
	}

	public static void syncConfig(Player player) {
		if (player.level().isClientSide) return;

		ServerPlayer target = (ServerPlayer) player;
		SOLPotPie.channel.sendTo(
				new ConfigMessage(),
				target.connection.connection,
				NetworkDirection.PLAY_TO_CLIENT
		);
	}
}
