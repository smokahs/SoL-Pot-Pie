package io.github.smokahs.solpotpie;

import io.github.smokahs.solpotpie.communication.ConfigMessage;
import io.github.smokahs.solpotpie.tracking.FoodInstance;
import io.github.smokahs.solpotpie.tracking.FoodScores;
import io.github.smokahs.solpotpie.utils.ComplexityParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID)
public class ConfigHandler {
	/** score of every food, computed from recipes + nutrition, with config overrides applied */
	public static Map<FoodInstance, Double> scoreMap = new HashMap<>();

	public final static String SCORE_MAP_KEY = "score_map";
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
		return tag;
	}

	public static void deserializeConfig(CompoundTag tag) {
		deserializeScoreMap(tag.getCompound(SCORE_MAP_KEY));
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
	}

	public static void rebuildScores(MinecraftServer server) {
		Map<FoodInstance, Double> newScoreMap = FoodScores.compute();
		newScoreMap.putAll(ComplexityParser.parse(SOLPotPieConfig.getScoreOverrides()));
		scoreMap = newScoreMap;
	}

	@SubscribeEvent
	public static void onServerStart(ServerStartingEvent event) {
		rebuildScores(event.getServer());

		isFirstAid = ModList.get().isLoaded("firstaid");
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
