package io.github.smokahs.solpotpie.foodgroups;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


public final class FoodGroups {
	public static final String FOLDER_NAME = SOLPotPie.MOD_ID;
	private static final String DEFAULTS_PATH = "/data/" + SOLPotPie.MOD_ID + "/food_groups/";
	private static final String EXAMPLE_NAME = "_example.json.txt";

	private static final String[] DEFAULT_GROUPS = {
		"fruit.json",
		"vegetables.json",
		"meat.json",
		"fish.json",
		"grain.json",
		"sweets.json",
		"soup.json",
		"foraged.json",
	};

	private static Map<String, String> definitions = Collections.emptyMap();

	private static List<FoodGroup> groups = Collections.emptyList();
	private static Map<Item, Set<FoodGroup>> groupsByFood = Collections.emptyMap();
	private static boolean hasBlacklistGroup = false;

	private static int generation = 0;

	private FoodGroups() {}

	public static void reload() {
		Path folder = folder();
		prepare(folder);

		Map<String, String> read = new LinkedHashMap<>();
		try (Stream<Path> files = Files.list(folder)) {
			files.filter(path -> path.getFileName().toString().endsWith(".json"))
				.sorted()
				.forEach(path -> read(path, read));
		} catch (IOException e) {
			SOLPotPie.LOGGER.warn("Could not list {}", folder, e);
		}

		definitions = read;
		apply();
	}


	public static void resolve() {
		if (definitions.isEmpty()) return;

		apply();
	}

	private static void apply() {
		List<FoodGroup> loaded = new ArrayList<>();
		for (Map.Entry<String, String> entry : definitions.entrySet()) {
			FoodGroup group = parse(entry.getKey(), entry.getValue());
			if (group != null && group.enabled()) {
				loaded.add(group);
			}
		}

		loaded.forEach(FoodGroup::resolve);

		Map<Item, Set<FoodGroup>> byFood = new HashMap<>();
		boolean blacklisted = false;
		for (FoodGroup group : loaded) {
			blacklisted |= group.blacklist();
			for (Item member : group.members()) {
				byFood.computeIfAbsent(member, key -> new HashSet<>()).add(group);
			}
		}

		groups = List.copyOf(loaded);
		groupsByFood = byFood;
		hasBlacklistGroup = blacklisted;
		generation++;

		SOLPotPie.LOGGER.info("Loaded {} food group(s) covering {} food(s)", groups.size(), groupsByFood.size());
	}

	@Nullable
	private static FoodGroup parse(String identifier, String json) {
		try {
			JsonElement element = JsonParser.parseString(json);
			return FoodGroup.parse(identifier, element);
		} catch (RuntimeException e) {
			SOLPotPie.LOGGER.warn("Skipping malformed food group {}: {}", identifier, e.getMessage());
			return null;
		}
	}

	private static Path folder() {
		return FMLPaths.CONFIGDIR.get().resolve(FOLDER_NAME);
	}

	private static void prepare(Path folder) {
		try {
			boolean fresh = !Files.isDirectory(folder);
			Files.createDirectories(folder);

			copyResource(EXAMPLE_NAME, folder.resolve(EXAMPLE_NAME), true);
			if (!fresh) return;

			for (String name : DEFAULT_GROUPS) {
				copyResource(name, folder.resolve(name), false);
			}
		} catch (IOException e) {
			SOLPotPie.LOGGER.warn("Could not prepare {}", folder, e);
		}
	}

	private static void copyResource(String name, Path target, boolean overwrite) throws IOException {
		if (!overwrite && Files.exists(target)) return;

		try (InputStream in = FoodGroups.class.getResourceAsStream(DEFAULTS_PATH + name)) {
			if (in == null) {
				SOLPotPie.LOGGER.warn("Missing bundled food group {}", name);
				return;
			}
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void read(Path path, Map<String, String> into) {
		String fileName = path.getFileName().toString();
		String identifier = fileName.substring(0, fileName.length() - ".json".length());

		try {
			into.put(identifier, Files.readString(path, StandardCharsets.UTF_8));
		} catch (IOException | RuntimeException e) {
			SOLPotPie.LOGGER.warn("Failed to read {}", path, e);
		}
	}

	
	public static CompoundTag serialize() {
		CompoundTag tag = new CompoundTag();
		for (Map.Entry<String, String> entry : definitions.entrySet()) {
			tag.putString(entry.getKey(), entry.getValue());
		}
		return tag;
	}

	public static void deserialize(CompoundTag tag) {
		Map<String, String> received = new LinkedHashMap<>();
		for (String identifier : tag.getAllKeys()) {
			received.put(identifier, tag.getString(identifier));
		}

		definitions = received;
		apply();
	}

	public static Collection<FoodGroup> all() {
		return groups;
	}

	public static int count() {
		return groups.size();
	}

	public static int generation() {
		return generation;
	}

	public static Set<FoodGroup> forFood(@Nullable Item food) {
		if (food == null) return Collections.emptySet();

		Set<FoodGroup> found = groupsByFood.get(food);
		return found == null ? Collections.emptySet() : found;
	}

	public static boolean belongsToAnyGroup(Item food) {
		return groupsByFood.containsKey(food);
	}


	public static boolean isExempt(Item food) {
		if (groupsByFood.isEmpty()) return false;

		boolean whitelistMode = SOLPotPieConfig.useFoodGroupsAsWhitelists();
		if (!hasBlacklistGroup && !whitelistMode) return false;

		Set<FoodGroup> found = forFood(food);
		if (whitelistMode && found.isEmpty()) return true;

		for (FoodGroup group : found) {
			if (group.blacklist()) return true;
		}
		return false;
	}

	public static int distinctGroups(Collection<Item> foods) {
		if (groupsByFood.isEmpty()) return 0;

		Set<FoodGroup> distinct = new HashSet<>();
		for (Item food : foods) {
			for (FoodGroup group : forFood(food)) {
				if (!group.blacklist()) {
					distinct.add(group);
				}
			}
		}
		return distinct.size();
	}

	public static void clear() {
		definitions = Collections.emptyMap();
		groups = Collections.emptyList();
		groupsByFood = Collections.emptyMap();
		hasBlacklistGroup = false;
		generation++;
	}
}
