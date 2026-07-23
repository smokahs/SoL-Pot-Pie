package io.github.smokahs.solpotpie;

import io.github.smokahs.solpotpie.tracking.CapabilityHandler;
import io.github.smokahs.solpotpie.tracking.HeartsHandler;
import com.google.common.collect.Lists;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.regex.Pattern;


@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID)
public final class SOLPotPieConfig
{
	private static String localizationPath(String path) {
		return "config." + SOLPotPie.MOD_ID + "." + path;
	}

	public static final Server SERVER;
	public static final ForgeConfigSpec SERVER_SPEC;

	static {
		Pair<Server, ForgeConfigSpec> specPair = new Builder().configure(Server::new);
		SERVER = specPair.getLeft();
		SERVER_SPEC = specPair.getRight();
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
		context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
		context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
	}

	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event) {
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
		return new ArrayList<>(SERVER.blacklist.get());
	}

	public static List<String> getWhitelist() {
		return new ArrayList<>(SERVER.whitelist.get());
	}

	public static List<String> getScoreOverrides() { return new ArrayList<>(SERVER.scoreOverrides.get()); }

	public static boolean shouldResetOnDeath() {
		return SERVER.shouldResetOnDeath.get();
	}

	public static boolean limitProgressionToSurvival() {
		return SERVER.limitProgressionToSurvival.get();
	}

	public static boolean shouldForbiddenCount() { return SERVER.shouldForbiddenCount.get(); }

	public static Integer size() {
		return SERVER.queueSize.get();
	}

	public static double baseHeartCost() { return SERVER.baseHeartCost.get(); }

	public static double heartCostIncrement() { return SERVER.heartCostIncrement.get(); }

	public static double healthPerHeart() { return SERVER.healthPerHeart.get(); }

	public static int maxHearts() { return SERVER.maxHearts.get(); }

	public static double scoreMultiplier() { return SERVER.scoreMultiplier.get(); }

	public static double maxScore() { return SERVER.maxScore.get(); }

	public static boolean diminishingReturnsEnabled() { return SERVER.diminishingReturnsEnabled.get(); }

	public static double diminishingFloor() { return SERVER.diminishingFloor.get(); }

	public static class Server {
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
		public final DoubleValue diminishingFloor;

		public final BooleanValue shouldForbiddenCount;

		public final ConfigValue<List<? extends String>> scoreOverrides;

		Server(Builder builder) {
			builder.push("Hearts");

			baseHeartCost = builder
					.translation(localizationPath("base_heart_cost"))
					.comment(" How many food score points the first heart costs.\n"
							+" Eating a food for the FIRST time permanently adds its score to your lifetime points.\n"
							+" Eating it again gives no further points.\n"
							+"\n")
					.defineInRange("baseHeartCost", 8.0, 0.1, 10000.0);

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
					.comment(" Every food is scored from its own nutrition and saturation (no crafting recipe walk).\n"
							+" saturation is counted once (nutrition x saturationModifier), then the score follows a\n"
							+" curve: it ramps up linearly to an average of 5, and compresses logarithmically above\n"
							+" that, so very filling foods can't run away with huge scores.\n"
							+" This multiplier scales every score up or down.\n"
							+" Raise it if permanent hearts feel too slow to earn, lower it if too fast.\n"
							+"\n")
					.defineInRange("scoreMultiplier", 1.0, 0.1, 100.0);

			maxScore = builder
					.translation(localizationPath("max_score"))
					.comment("\n Scores are clamped to this maximum.\n"
							+"\n")
					.defineInRange("maxScore", 10.0, 1.0, 1000.0);

			scoreOverrides = builder
					.translation(localizationPath("score_overrides"))
					.comment("\n Override the automatically computed score of individual foods here.\n"
							+" Use this to hand-tune special or modded foods whose nutrition-based score doesn't fit.\n"
							+" Each entry is a string of the form [registry name],[score]\n"
							+" e.g. \"minecraft:enchanted_golden_apple,10\"\n"
							+" Note that tags are NOT currently supported.\n"
							+"\n")
					.defineList("scoreOverrides", Lists.newArrayList(
									"minecraft:enchanted_golden_apple,10"),
							e -> e instanceof String);

			builder.pop();
			builder.push("DiminishingReturns");

			diminishingReturnsEnabled = builder
					.translation(localizationPath("diminishing_returns_enabled"))
					.comment(" If true, eating a food you already ate recently restores less hunger and saturation.\n"
							+" The more recently you ate it, the less it restores.\n"
							+"\n")
					.define("diminishingReturnsEnabled", true);

			diminishingFloor = builder
					.translation(localizationPath("diminishing_floor"))
					.comment("\n The lowest hunger/saturation multiplier possible, applied when re-eating a food\n"
							+" you JUST ate. Scales back up to 1.0 as the food ages out of the recent-food queue.\n"
							+"\n")
					.defineInRange("diminishingFloor", 0.3, 0.0, 1.0);

			queueSize = builder
					.translation(localizationPath("queue_size"))
					.comment("\n How many meals count as \"recent\" for diminishing returns and the food book queue.\n"
							+"\n")
					.defineInRange("queueSize", 128, 1, 1000);

			builder.pop();
			builder.push("Filtering");

			blacklist = builder
					.translation(localizationPath("blacklist"))
					.comment(" Foods in this list won't give points or appear in the food queue.\n"
							+"\n")
					.defineList("blacklist", Lists.newArrayList(), e -> e instanceof String);

			whitelist = builder
					.translation(localizationPath("whitelist"))
					.comment("\n When this list contains anything, the blacklist is ignored and instead only foods from here count.\n"
							+"\n")
					.defineList("whitelist", Lists.newArrayList(), e -> e instanceof String);

			shouldForbiddenCount = builder
					.translation(localizationPath("should_forbidden_count"))
					.comment("\n Whether blacklisted foods should still take a spot in the recent-food queue,\n"
							+" even though they don't give any points.\n"
							+"\n")
					.define("shouldForbiddenCount", true);

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
		String id = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(food)).toString();
		return !matchesAnyPattern(id, CLIENT.tooltipBlacklist.get());
	}

	public static class Client {
		public final BooleanValue isFoodTooltipEnabled;
		public final BooleanValue hideValuesUntilEaten;
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

			tooltipBlacklist = builder
				.translation(localizationPath("tooltip_blacklist"))
				.comment("\n Items in this list never get any Spice of Life tooltip line, including the\n"
						+" \"Not yet eaten\" one. Use it for edible items that aren't really meals,\n"
						+" e.g. lunchboxes or machine/placeholder foods.\n"
						+" Each entry is a registry name, e.g. \"minecraft:bread\".\n"
						+" Supports * wildcards, e.g. \"solpotpie:*\" covers every item of this mod.\n"
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

	// TODO: investigate performance of all these get() calls

	public static boolean hasWhitelist() {
		return !SERVER.whitelist.get().isEmpty();
	}

	public static boolean isAllowed(Item food) {
		String id = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(food)).toString();
		if (hasWhitelist()) {
			return matchesAnyPattern(id, SERVER.whitelist.get());
		} else {
			return !matchesAnyPattern(id, SERVER.blacklist.get());
		}
	}

	public static boolean shouldCount(Item food) {
		return isAllowed(food);
	}

	private static boolean matchesAnyPattern(String query, Collection<? extends String> patterns) {
		for (String glob : patterns) {
			StringBuilder pattern = new StringBuilder(glob.length());
			for (String part : glob.split("\\*", -1)) {
				if (!part.isEmpty()) { // not necessary
					pattern.append(Pattern.quote(part));
				}
				pattern.append(".*");
			}

			// delete extraneous trailing ".*" wildcard
			pattern.delete(pattern.length() - 2, pattern.length());

			if (Pattern.matches(pattern.toString(), query)) {
				return true;
			}
		}
		return false;
	}
}
