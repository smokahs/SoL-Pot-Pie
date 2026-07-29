package io.github.smokahs.solpotpie;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class Sync {
	public static final String QUEUE_SIZE = "queue_size";
	public static final String SHOULD_RESET_ON_DEATH = "reset_on_death";
	public static final String LIMIT_PROGRESSION_TO_SURVIVAL = "limit_progression_to_survival";
	public static final String BASE_HEART_COST = "base_heart_cost";
	public static final String HEART_COST_INCREMENT = "heart_cost_increment";
	public static final String HEALTH_PER_HEART = "health_per_heart";
	public static final String MAX_HEARTS = "max_hearts";
	public static final String SCORE_MULTIPLIER = "score_multiplier";
	public static final String MAX_SCORE = "max_score";
	public static final String DIMINISHING_ENABLED = "diminishing_returns_enabled";
	public static final String DIMINISHING_EATS_TO_FLOOR = "diminishing_eats_to_floor";
	public static final String DIMINISHING_FLOOR_HUNGER = "diminishing_floor_hunger";
	public static final String DIMINISHING_FLOOR_SATURATION = "diminishing_floor_saturation";
	public static final String DIMINISHING_RECOVERY_VAL = "diminishing_recovery_val";
	public static final String FOOD_GROUP_DIVERSITY_THRESHOLD = "food_group_diversity_threshold";
	public static final String USE_FOOD_GROUPS_AS_WHITELISTS = "use_food_groups_as_whitelists";
	public static final String NEW_PLAYER_FOODS_EATEN_THRESHOLD = "new_player_foods_eaten_threshold";
	public static final String SHOULD_FORBIDDEN_COUNT = "should_forbidden_count";
	public static final String BLACKLIST = "blacklist";
	public static final String WHITELIST = "whitelist";

	private static CompoundTag values = null;

	private static List<String> blacklist = null;
	private static List<String> whitelist = null;

	private Sync() {}

	public static CompoundTag serialize() {
		SOLPotPieConfig.Common common = SOLPotPieConfig.COMMON;
		CompoundTag tag = new CompoundTag();

		tag.putInt(QUEUE_SIZE, common.queueSize.get());
		tag.putBoolean(SHOULD_RESET_ON_DEATH, common.shouldResetOnDeath.get());
		tag.putBoolean(LIMIT_PROGRESSION_TO_SURVIVAL, common.limitProgressionToSurvival.get());

		tag.putDouble(BASE_HEART_COST, common.baseHeartCost.get());
		tag.putDouble(HEART_COST_INCREMENT, common.heartCostIncrement.get());
		tag.putDouble(HEALTH_PER_HEART, common.healthPerHeart.get());
		tag.putInt(MAX_HEARTS, common.maxHearts.get());

		tag.putDouble(SCORE_MULTIPLIER, common.scoreMultiplier.get());
		tag.putDouble(MAX_SCORE, common.maxScore.get());

		tag.putBoolean(DIMINISHING_ENABLED, common.diminishingReturnsEnabled.get());
		tag.putInt(DIMINISHING_EATS_TO_FLOOR, common.diminishingEatsToFloor.get());
		tag.putInt(DIMINISHING_FLOOR_HUNGER, common.diminishingFloorHunger.get());
		tag.putDouble(DIMINISHING_FLOOR_SATURATION, common.diminishingFloorSaturation.get());
		tag.putDouble(DIMINISHING_RECOVERY_VAL, common.diminishingRecoveryVal.get());
		tag.putInt(FOOD_GROUP_DIVERSITY_THRESHOLD, common.foodGroupDiversityThreshold.get());
		tag.putBoolean(USE_FOOD_GROUPS_AS_WHITELISTS, common.useFoodGroupsAsWhitelists.get());
		tag.putInt(NEW_PLAYER_FOODS_EATEN_THRESHOLD, common.newPlayerFoodsEatenThreshold.get());

		tag.putBoolean(SHOULD_FORBIDDEN_COUNT, common.shouldForbiddenCount.get());
		tag.put(BLACKLIST, strings(common.blacklist.get()));
		tag.put(WHITELIST, strings(common.whitelist.get()));

		return tag;
	}

	public static void accept(CompoundTag tag) {
		values = tag == null || tag.isEmpty() ? null : tag;
		blacklist = values == null ? null : parse(BLACKLIST);
		whitelist = values == null ? null : parse(WHITELIST);
	}

	public static void clear() {
		values = null;
		blacklist = null;
		whitelist = null;
	}

	static boolean flag(String key, boolean fallback) {
		return values != null && values.contains(key) ? values.getBoolean(key) : fallback;
	}

	static int integer(String key, int fallback) {
		return values != null && values.contains(key) ? values.getInt(key) : fallback;
	}

	static double number(String key, double fallback) {
		return values != null && values.contains(key) ? values.getDouble(key) : fallback;
	}

	static List<String> list(String key, List<? extends String> fallback) {
		return new ArrayList<>(raw(key, fallback));
	}

	static List<? extends String> raw(String key, List<? extends String> fallback) {
		if (values == null) return fallback;

		List<String> synced = BLACKLIST.equals(key) ? blacklist : WHITELIST.equals(key) ? whitelist : null;
		return synced == null ? fallback : synced;
	}

	private static List<String> parse(String key) {
		if (!values.contains(key)) return null;

		ListTag entries = values.getList(key, Tag.TAG_STRING);
		List<String> result = new ArrayList<>(entries.size());
		for (int i = 0; i < entries.size(); i++) {
			result.add(entries.getString(i));
		}
		return result;
	}

	private static ListTag strings(List<? extends String> source) {
		ListTag list = new ListTag();
		for (String entry : source) {
			list.add(StringTag.valueOf(entry));
		}
		return list;
	}
}
