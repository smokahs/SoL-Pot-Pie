package io.github.smokahs.solpotpie;

import io.github.smokahs.solpotpie.tracking.CapabilityHandler;
import io.github.smokahs.solpotpie.tracking.HeartsHandler;
import io.github.smokahs.solpotpie.tracking.PackTotals;
import com.google.common.collect.Lists;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SOLPotPieConfig
{
	private static final String HUNGER_OVERHAULED = "hungeroverhauled";

	private static final TagKey<Item> DISABLED_TAG =
			TagKey.create(Registries.ITEM, new ResourceLocation(SOLPotPie.MOD_ID, "disabled"));

	private static final double BASE_HEART_COST_DEFAULT = 10.0;
	private static final double BASE_HEART_COST_HUNGER_OVERHAULED = 6.0;

	// two rows of 7 is all the container GUI fits
	public static final int MAX_CONTAINER_SLOTS = 14;

	private static String localizationPath(String path) {
		return "config." + SOLPotPie.MOD_ID + "." + path;
	}

	/**
	Whether Hunger Overhauled is present. Read while the config spec is built, which is early
	enough that the mod list is worth a null check even though it should be up by then.
	 */
	private static boolean hasHungerOverhauled() {
		ModList mods = ModList.get();
		return mods != null && mods.isLoaded(HUNGER_OVERHAULED);
	}

	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;

	static {
		Pair<Common, ForgeConfigSpec> specPair = new Builder().configure(Common::new);
		COMMON = specPair.getLeft();
		COMMON_SPEC = specPair.getRight();
	}

	public static final Client CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;

	static {
		Pair<Client, ForgeConfigSpec> specPair = new Builder().configure(Client::new);
		CLIENT = specPair.getLeft();
		CLIENT_SPEC = specPair.getRight();
	}

	public static void setUp() {
		ModLoadingContext context = ModLoadingContext.get();
		context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
		context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
	}

	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event) {
		if (event.getConfig().getType() != ModConfig.Type.COMMON) return;

		PackTotals.invalidate();

		MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
		if (currentServer == null) return;

		ConfigHandler.rebuildScores(currentServer);

		PlayerList players = currentServer.getPlayerList();
		for (Player player : players.getPlayers()) {
			HeartsHandler.updatePlayer(player);
			CapabilityHandler.syncFoodList(player);
			ConfigHandler.syncConfig(player);
		}
	}

	public static List<String> getBlacklist() {
		return Sync.list(Sync.BLACKLIST, COMMON.blacklist.get());
	}

	public static List<String> getWhitelist() {
		return Sync.list(Sync.WHITELIST, COMMON.whitelist.get());
	}

	public static List<String> getScoreOverrides() { return new ArrayList<>(COMMON.scoreOverrides.get()); }

	public static boolean shouldResetOnDeath() {
		return Sync.flag(Sync.SHOULD_RESET_ON_DEATH, COMMON.shouldResetOnDeath.get());
	}

	public static boolean limitProgressionToSurvival() {
		return Sync.flag(Sync.LIMIT_PROGRESSION_TO_SURVIVAL, COMMON.limitProgressionToSurvival.get());
	}

	public static boolean shouldForbiddenCount() {
		return Sync.flag(Sync.SHOULD_FORBIDDEN_COUNT, COMMON.shouldForbiddenCount.get());
	}

	public static Integer size() {
		return Sync.integer(Sync.QUEUE_SIZE, COMMON.queueSize.get());
	}

	public static double baseHeartCost() {
		return Sync.number(Sync.BASE_HEART_COST, COMMON.baseHeartCost.get());
	}

	public static double heartCostIncrement() {
		return Sync.number(Sync.HEART_COST_INCREMENT, COMMON.heartCostIncrement.get());
	}

	public static double healthPerHeart() {
		return Sync.number(Sync.HEALTH_PER_HEART, COMMON.healthPerHeart.get());
	}

	public static int maxHearts() {
		return Sync.integer(Sync.MAX_HEARTS, COMMON.maxHearts.get());
	}

	public static double scoreMultiplier() {
		return Sync.number(Sync.SCORE_MULTIPLIER, COMMON.scoreMultiplier.get());
	}

	public static double maxScore() {
		return Sync.number(Sync.MAX_SCORE, COMMON.maxScore.get());
	}

	public static boolean diminishingReturnsEnabled() {
		return Sync.flag(Sync.DIMINISHING_ENABLED, COMMON.diminishingReturnsEnabled.get());
	}

	public static int diminishingEatsToFloor() {
		return Sync.integer(Sync.DIMINISHING_EATS_TO_FLOOR, COMMON.diminishingEatsToFloor.get());
	}

	public static int diminishingFloorHunger() {
		return Sync.integer(Sync.DIMINISHING_FLOOR_HUNGER, COMMON.diminishingFloorHunger.get());
	}

	public static double diminishingFloorSaturation() {
		return Sync.number(Sync.DIMINISHING_FLOOR_SATURATION, COMMON.diminishingFloorSaturation.get());
	}

	public static double diminishingRecoveryVal() {
		return Sync.number(Sync.DIMINISHING_RECOVERY_VAL, COMMON.diminishingRecoveryVal.get());
	}

	public static boolean diminishingEatsToFloorAddsNutrition() {
		return Sync.flag(Sync.DIMINISHING_EATS_ADD_NUTRITION, COMMON.diminishingEatsToFloorAddsNutrition.get());
	}

	public static boolean diminishedEatTimeScaling() {
		return Sync.flag(Sync.DIMINISHED_EAT_TIME_SCALING, COMMON.diminishedEatTimeScaling.get());
	}

	public static int foodGroupDiversityThreshold() {
		return Sync.integer(Sync.FOOD_GROUP_DIVERSITY_THRESHOLD, COMMON.foodGroupDiversityThreshold.get());
	}

	public static boolean useFoodGroupsAsWhitelists() {
		return Sync.flag(Sync.USE_FOOD_GROUPS_AS_WHITELISTS, COMMON.useFoodGroupsAsWhitelists.get());
	}

	public static int newPlayerFoodsEatenThreshold() {
		return Sync.integer(Sync.NEW_PLAYER_FOODS_EATEN_THRESHOLD, COMMON.newPlayerFoodsEatenThreshold.get());
	}

	public static int lunchbagSlots() {
		return Sync.integer(Sync.LUNCHBAG_SLOTS, COMMON.lunchbagSlots.get());
	}

	public static int lunchboxSlots() {
		return Sync.integer(Sync.LUNCHBOX_SLOTS, COMMON.lunchboxSlots.get());
	}

	public static int goldenLunchboxSlots() {
		return Sync.integer(Sync.GOLDEN_LUNCHBOX_SLOTS, COMMON.goldenLunchboxSlots.get());
	}

	public static class Common {
		public final ConfigValue<List<? extends String>> blacklist;
		public final ConfigValue<List<? extends String>> whitelist;

		public final IntValue queueSize;

		public final BooleanValue shouldResetOnDeath;
		public final BooleanValue limitProgressionToSurvival;

		public final DoubleValue baseHeartCost;
		public final DoubleValue heartCostIncrement;
		public final DoubleValue healthPerHeart;
		public final IntValue maxHearts;

		public final DoubleValue scoreMultiplier;
		public final DoubleValue maxScore;

		public final BooleanValue diminishingReturnsEnabled;
		public final IntValue diminishingEatsToFloor;
		public final IntValue diminishingFloorHunger;
		public final DoubleValue diminishingFloorSaturation;
		public final DoubleValue diminishingRecoveryVal;
		public final BooleanValue diminishingEatsToFloorAddsNutrition;
		public final BooleanValue diminishedEatTimeScaling;
		public final IntValue foodGroupDiversityThreshold;
		public final BooleanValue useFoodGroupsAsWhitelists;
		public final IntValue newPlayerFoodsEatenThreshold;

		public final BooleanValue shouldForbiddenCount;

		public final IntValue lunchbagSlots;
		public final IntValue lunchboxSlots;
		public final IntValue goldenLunchboxSlots;

		public final ConfigValue<List<? extends String>> scoreOverrides;

		Common(Builder builder) {
			builder.push("Hearts");

			baseHeartCost = builder
					.translation(localizationPath("base_heart_cost"))
					.comment(" How many food score points the first heart costs. Only the FIRST time you eat a\n"
							+" food adds its score. Ships at " + BASE_HEART_COST_DEFAULT + ", or "
									+ BASE_HEART_COST_HUNGER_OVERHAULED + " with Hunger Overhauled installed.\n"
							+" Defaults apply only when this file is first written; installing it later changes nothing.\n"
							+"\n")
					.defineInRange("baseHeartCost",
							hasHungerOverhauled()
									? BASE_HEART_COST_HUNGER_OVERHAULED
									: BASE_HEART_COST_DEFAULT,
							0.1, 10000.0);

			heartCostIncrement = builder
					.translation(localizationPath("heart_cost_increment"))
					.comment("\n How much MORE each subsequent heart costs than the previous one.\n"
							+" 0 = every heart costs baseHeartCost (linear growth).\n"
							+" e.g. 2 = hearts cost 10, 12, 14, 16, ...\n"
							+"\n")
					.defineInRange("heartCostIncrement", 0.0, 0.0, 1000.0);

			healthPerHeart = builder
					.translation(localizationPath("health_per_heart"))
					.comment("\n How much max health each earned heart grants. 2 = one full heart.\n"
							+"\n")
					.defineInRange("healthPerHeart", 2.0, 0.0, 100.0);

			maxHearts = builder
					.translation(localizationPath("max_hearts"))
					.comment("\n Maximum number of hearts that can be earned. 0 = unlimited.\n"
							+"\n")
					.defineInRange("maxHearts", 0, 0, 1000);

			builder.pop();
			builder.push("Scoring");

			scoreMultiplier = builder
					.translation(localizationPath("score_multiplier"))
					.comment(" Every food is scored from its own nutrition and saturation.\n"
							+" Raise it if permanent hearts feel too slow to earn, lower it if too fast.\n"
							+"\n")
					.defineInRange("scoreMultiplier", 1.0, 0.1, 100.0);

			maxScore = builder
					.translation(localizationPath("max_score"))
					.comment("\n The maximum score a single food can contribute to lifetime point total.\n"
							+"\n")
					.defineInRange("maxScore", 10.0, 1.0, 1000.0);

			scoreOverrides = builder
					.translation(localizationPath("score_overrides"))
					.comment("\n Use this to hand-tune special or modded foods whose nutrition-based score doesn't fit.\n"
							+" Each entry is a string of the form [registry name],[score]\n"
							+" e.g. \"minecraft:enchanted_golden_apple,10\"\n"
							+" tags are NOT supported.\n"
							+"\n")
					.defineList("scoreOverrides", Lists.newArrayList(
									"minecraft:enchanted_golden_apple,10"),
							e -> e instanceof String);

			builder.pop();
			builder.push("DiminishingReturns");

			diminishingReturnsEnabled = builder
					.translation(localizationPath("diminishing_returns_enabled"))
					.comment(" If true, eating a food you already ate recently restores less hunger and saturation.\n"
							+" The more times you have eaten it, the less it restores.\n"
							+"\n")
					.define("diminishingReturnsEnabled", true);

			diminishingEatsToFloor = builder
					.translation(localizationPath("diminishing_eats_to_floor"))
					.comment("\n How many times a food has to be eaten before it bottoms out. The first eat is\n"
							+" always worth the food's full value, and each eat after that slides linearly down\n"
							+" to the floor below, which is reached on this eat and stays there.\n"
							+" The count only resets once the food ages out of the recent-food queue entirely,\n"
							+" so it is tied to queueSize.\n"
							+"\n")
					.defineInRange("diminishingEatsToFloor", 5, 2, 100);

			diminishingFloorHunger = builder
					.translation(localizationPath("diminishing_floor_hunger"))
					.comment("\n Hunger a food restores once it has bottomed out, no matter how filling it is.\n"
							+" 1 is half a shank. This is a flat amount, not a multiplier, so steak and bread\n"
							+" end up equally worthless when spammed.\n"
							+"\n")
					.defineInRange("diminishingFloorHunger", 1, 0, 20);

			diminishingRecoveryVal = builder
					.translation(localizationPath("diminishing_recovery_val"))
					.comment("\n How fast a worn-out food climbs back to its full value as you eat other things.\n"
							+" 1.0 spreads the whole recovery across queueSize meals, so a food is fully back to\n"
							+" normal right as it ages out of the recent-food queue.\n"
							+" Below 1.0 is slower: at 0.5 only half the penalty is walked off by the time the\n"
							+" food ages out, and the rest goes at once. Above 1.0 is faster: at 2.0 a food is\n"
							+" fully recovered after half a queue's worth of meals.\n"
							+" 0.0 disables recovery entirely, so a food only resets by aging out.\n"
							+"\n")
					.defineInRange("diminishingRecoveryVal", 1.0, 0.0, 2.0);

			diminishingFloorSaturation = builder
					.translation(localizationPath("diminishing_floor_saturation"))
					.comment("\n Saturation a food restores once it has bottomed out. Also a flat amount.\n"
							+"\n")
					.defineInRange("diminishingFloorSaturation", 0.5, 0.0, 20.0);

			diminishingEatsToFloorAddsNutrition = builder
					.translation(localizationPath("diminishing_eats_to_floor_adds_nutrition"))
					.comment("\n If true, a food's own hunger value is added to diminishingEatsToFloor, so junk\n"
							+" food bottoms out after a couple of eats while filling food holds value longer.\n"
							+" For the old 1.7.10 editions' feel, pair with floors of 0 and recovery 0.0.\n"
							+"\n")
					.define("diminishingEatsToFloorAddsNutrition", false);

			diminishedEatTimeScaling = builder
					.translation(localizationPath("diminished_eat_time_scaling"))
					.comment("\n If true, worn-out foods take longer to eat: eating time is divided by the\n"
							+" food's current fraction of its full hunger, uncapped. Half value chews twice\n"
							+" as long, and a food bottomed out at 0 hunger is effectively uneatable.\n"
							+"\n")
					.define("diminishedEatTimeScaling", false);

			queueSize = builder
					.translation(localizationPath("queue_size"))
					.comment("\n How many meals count as \"recent\" for diminishing returns and the food book queue.\n"
							+" This is also how long a worn-out food takes to recover, since diminishingRecoveryVal\n"
							+" spreads recovery across the whole queue. Keep it short enough that a food you stopped\n"
							+" eating comes back within a reasonable number of meals.\n"
							+"\n")
					.defineInRange("queueSize", 20, 1, 1000);

			newPlayerFoodsEatenThreshold = builder
					.translation(localizationPath("new_player_foods_eaten_threshold"))
					.comment("\n How many foods a player has to eat in a world before diminishing returns start\n"
							+" applying to them at all. Gives new characters a grace period to get set up before\n"
							+" they have to think about variety. 0 disables the grace period.\n"
							+" Counted per player per world, and not reset by death.\n"
							+"\n")
					.defineInRange("newPlayerFoodsEatenThreshold", 10, 0, 1000);

			builder.pop();
			builder.push("FoodGroups");

			foodGroupDiversityThreshold = builder
					.translation(localizationPath("food_group_diversity_threshold"))
					.comment(" While your recent meals cover this^ many DISTINCT food groups or fewer, diminishing\n"
							+" returns apply as normal. Cover more groups than this^ and diminishing returns get turned\n"
							+" off entirely.\n"
							+"\n"
							+" Set to 0 to ignore food groups and always apply diminishing returns.\n"
							+"\n")
					.defineInRange("foodGroupDiversityThreshold", 5, 0, 1000);

			useFoodGroupsAsWhitelists = builder
					.translation(localizationPath("use_food_groups_as_whitelists"))
					.comment("\n If true, any food that belongs to no food group at all is excluded from diminishing\n"
							+" returns, so only foods you have explicitly grouped can ever diminish.\n"
							+" If false, ungrouped foods diminish as usual, they just never add to your variety\n"
							+" count. Leave this off unless your food groups cover the whole pack.\n"
							+" Either way, a group marked \"blacklist\" always exempts its foods.\n"
							+"\n")
					.define("useFoodGroupsAsWhitelists", false);

			builder.pop();
			builder.push("Filtering");

			blacklist = builder
					.translation(localizationPath("blacklist"))
					.comment(" Foods in this list won't give points or appear in the food queue.\n"
							+" Entries are registry names with * wildcards (\"somemod:*\"), or item tags\n"
							+" prefixed with # (\"#forge:crops\"). Items tagged #solpotpie:disabled by a\n"
							+" datapack are always excluded, even when a whitelist is set.\n"
							+"\n")
					.defineList("blacklist", Lists.newArrayList(), e -> e instanceof String);

			whitelist = builder
					.translation(localizationPath("whitelist"))
					.comment("\n When this list contains anything, the blacklist is ignored and instead only foods from here count.\n"
							+" Same syntax as the blacklist: registry names, * wildcards, or #item tags.\n"
							+"\n")
					.defineList("whitelist", Lists.newArrayList(), e -> e instanceof String);

			shouldForbiddenCount = builder
					.translation(localizationPath("should_forbidden_count"))
					.comment("\n Whether blacklisted foods should still take a spot in the recent-food queue,\n"
							+" even though they don't give any points.\n"
							+"\n")
					.define("shouldForbiddenCount", true);

			builder.pop();
			builder.push("Lunchboxes");

			lunchbagSlots = builder
					.translation(localizationPath("lunchbag_slots"))
					.comment(" Food slots per container, 0-" + MAX_CONTAINER_SLOTS + ".\n"
							+" Existing containers resize when their stack next loads; shrinking voids\n"
							+" whatever sat past the new size.\n"
							+"\n")
					.defineInRange("lunchbagSlots", 5, 0, MAX_CONTAINER_SLOTS);

			lunchboxSlots = builder
					.translation(localizationPath("lunchbox_slots"))
					.defineInRange("lunchboxSlots", 9, 0, MAX_CONTAINER_SLOTS);

			goldenLunchboxSlots = builder
					.translation(localizationPath("golden_lunchbox_slots"))
					.defineInRange("goldenLunchboxSlots", 14, 0, MAX_CONTAINER_SLOTS);

			builder.pop();
			builder.push("Miscellaneous");

			shouldResetOnDeath = builder
					.translation(localizationPath("reset_on_death"))
					.comment(" Whether to reset the RECENT food queue on death (affects diminishing returns only).\n"
							+" Earned hearts are permanent and are never lost on death.\n"
							+"\n")
					.define("resetOnDeath", false);

			limitProgressionToSurvival = builder
					.translation(localizationPath("limit_progression_to_survival"))
					.comment("\n If true, eating foods outside of survival mode (e.g. creative/adventure) is not tracked.\n"
							+"\n")
					.define("limitProgressionToSurvival", false);

			builder.pop();
		}
	}

	public static boolean isFoodTooltipEnabled() {
		return CLIENT.isFoodTooltipEnabled.get();
	}

	public static boolean shouldHideValuesUntilEaten() {
		return CLIENT.hideValuesUntilEaten.get();
	}

	public static boolean hasTooltip(Item food) {
		return !matchesAnyEntry(food, CLIENT.tooltipBlacklist.get());
	}

	public static boolean modTooltipSupportEnabled() {
		return CLIENT.modTooltipSupport.get();
	}

	public static class Client {
		public final BooleanValue isFoodTooltipEnabled;
		public final BooleanValue hideValuesUntilEaten;
		public final BooleanValue modTooltipSupport;
		public final ConfigValue<List<? extends String>> tooltipBlacklist;

		Client(Builder builder) {
			builder.push("miscellaneous");

			isFoodTooltipEnabled = builder
				.translation(localizationPath("is_food_tooltip_enabled"))
				.comment(" If true, foods show their score and eaten status in their tooltips."
						+"\n")
				.define("isFoodTooltipEnabled", true);

			hideValuesUntilEaten = builder
				.translation(localizationPath("hide_values_until_eaten"))
				.comment("\n If true, a food's score is hidden until you have eaten it at least once.\n"
						+" Unknown foods just show a mysterious flavor line.\n"
						+"\n")
				.define("hideValuesUntilEaten", true);

			modTooltipSupport = builder
				.translation(localizationPath("mod_tooltip_support"))
				.comment("\n If true, food tooltip lines from the various mods with support\n"
						+" are hidden until the food has been eaten, and only shown while\n"
						+" holding shift\n"
						+"\n")
				.define("modTooltipSupport", true);

			tooltipBlacklist = builder
				.translation(localizationPath("tooltip_blacklist"))
				.comment("\n Items in this list never get any Spice of Life tooltip line, including the\n"
						+" \"Not yet eaten\" one. Use it for edible items that aren't really meals,\n"
						+" e.g. lunchboxes or machine/placeholder foods.\n"
						+" Each entry is a registry name, e.g. \"minecraft:bread\".\n"
						+" Supports * wildcards (\"solpotpie:*\") and #item tags (\"#forge:crops\").\n"
						+" This is cosmetic only: it does not change scoring or tracking.\n"
						+"\n")
				.defineList("tooltipBlacklist", Lists.newArrayList(
								"solpotpie:lunchbox",
								"solpotpie:lunchbag",
								"solpotpie:golden_lunchbox"),
						e -> e instanceof String);

			builder.pop();
		}
	}

	public static boolean hasWhitelist() {
		return !Sync.raw(Sync.WHITELIST, COMMON.whitelist.get()).isEmpty();
	}

	public static boolean isAllowed(Item food) {
		if (isTagged(food, DISABLED_TAG)) {
			return false;
		}
		if (hasWhitelist()) {
			return matchesAnyEntry(food, Sync.raw(Sync.WHITELIST, COMMON.whitelist.get()));
		} else {
			return !matchesAnyEntry(food, Sync.raw(Sync.BLACKLIST, COMMON.blacklist.get()));
		}
	}

	public static boolean shouldCount(Item food) {
		return isAllowed(food);
	}

	private static boolean matchesAnyEntry(Item food, Collection<? extends String> entries) {
		String id = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(food)).toString();
		for (String entry : entries) {
			if (entry.startsWith("#")) {
				ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
				if (tagId != null && isTagged(food, TagKey.create(Registries.ITEM, tagId))) {
					return true;
				}
			} else if (matchesPattern(id, entry)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isTagged(Item food, TagKey<Item> tag) {
		ITagManager<Item> tags = ForgeRegistries.ITEMS.tags();
		return tags != null && tags.getTag(tag).contains(food);
	}

	private static boolean matchesPattern(String query, String glob) {
		StringBuilder pattern = new StringBuilder(glob.length());
		for (String part : glob.split("\\*", -1)) {
			if (!part.isEmpty()) {
				pattern.append(Pattern.quote(part));
			}
			pattern.append(".*");
		}

		pattern.delete(pattern.length() - 2, pattern.length());

		return Pattern.matches(pattern.toString(), query);
	}
}
