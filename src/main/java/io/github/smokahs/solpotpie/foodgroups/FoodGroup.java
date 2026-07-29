package io.github.smokahs.solpotpie.foodgroups;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.smokahs.solpotpie.SOLPotPie;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class FoodGroup {
	private static final ChatFormatting DEFAULT_COLOR = ChatFormatting.GRAY;

	private final String identifier;
	@Nullable
	private final String name;
	private final boolean enabled;
	private final boolean blacklist;
	private final boolean hidden;
	private final ChatFormatting color;

	private final List<String> includedItems;
	private final List<String> includedTags;
	private final List<String> excludedItems;
	private final List<String> excludedTags;

	private final Set<Item> members = new HashSet<>();

	private FoodGroup(String identifier, JsonObject json) {
		this.identifier = identifier;
		this.name = GsonHelper.getAsString(json, "name", null);
		this.enabled = GsonHelper.getAsBoolean(json, "enabled", true);
		this.blacklist = GsonHelper.getAsBoolean(json, "blacklist", false);
		// blacklist groups are never worth showing, same as the mod this is modelled on
		this.hidden = GsonHelper.getAsBoolean(json, "hidden", false) || this.blacklist;
		this.color = parseColor(GsonHelper.getAsString(json, "color", null));

		JsonObject food = GsonHelper.getAsJsonObject(json, "food", new JsonObject());
		JsonObject exclude = GsonHelper.getAsJsonObject(json, "exclude", new JsonObject());

		this.includedItems = strings(food, "items");
		this.includedTags = strings(food, "tags");
		this.excludedItems = strings(exclude, "items");
		this.excludedTags = strings(exclude, "tags");

		// "oredict" was the 1.7.10 spelling of "tags"; accept it so old food groups can be pasted in
		this.includedTags.addAll(strings(food, "oredict"));
		this.excludedTags.addAll(strings(exclude, "oredict"));
	}

	@Nullable
	public static FoodGroup parse(String identifier, JsonElement element) {
		if (!element.isJsonObject()) {
			SOLPotPie.LOGGER.warn("Skipping food group {}: root element is not an object", identifier);
			return null;
		}

		try {
			return new FoodGroup(identifier, element.getAsJsonObject());
		} catch (RuntimeException e) {
			SOLPotPie.LOGGER.warn("Skipping malformed food group {}: {}", identifier, e.getMessage());
			return null;
		}
	}

	private static List<String> strings(JsonObject json, String key) {
		List<String> values = new ArrayList<>();
		JsonArray array = GsonHelper.getAsJsonArray(json, key, null);
		if (array == null) return values;

		for (JsonElement entry : array) {
			if (entry.isJsonPrimitive()) {
				values.add(entry.getAsString());
			}
		}
		return values;
	}

	private static ChatFormatting parseColor(@Nullable String name) {
		if (name == null) return DEFAULT_COLOR;

		ChatFormatting formatting = ChatFormatting.getByName(name);
		return formatting == null ? DEFAULT_COLOR : formatting;
	}

	/** resolves item ids and tags into a flat member set. tags only exist once they are bound. */
	void resolve() {
		members.clear();

		Set<Item> included = new HashSet<>();
		collect(includedItems, includedTags, included);

		Set<Item> excluded = new HashSet<>();
		collect(excludedItems, excludedTags, excluded);

		included.removeAll(excluded);
		members.addAll(included);
	}

	private void collect(List<String> itemIds, List<String> tagIds, Set<Item> into) {
		for (String id : itemIds) {
			if (id.startsWith("#")) {
				addTag(id.substring(1), into);
				continue;
			}

			Item item = item(id);
			if (item == null) continue;
			into.add(item);
		}

		for (String id : tagIds) {
			addTag(id.startsWith("#") ? id.substring(1) : id, into);
		}
	}

	@Nullable
	private Item item(String id) {
		ResourceLocation name = resourceLocation(id);
		if (name == null) return null;

		Item item = ForgeRegistries.ITEMS.getValue(name);
		if (item == null || !ForgeRegistries.ITEMS.containsKey(name)) {
			SOLPotPie.LOGGER.debug("Food group {}: item {} is not registered, ignoring", identifier, id);
			return null;
		}
		return item;
	}

	private void addTag(String id, Set<Item> into) {
		ResourceLocation name = resourceLocation(id);
		if (name == null) return;

		ITag<Item> tag = ForgeRegistries.ITEMS.tags() == null
			? null
			: ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, name));
		if (tag == null || tag.isEmpty()) {
			SOLPotPie.LOGGER.debug("Food group {}: tag #{} is empty, ignoring", identifier, id);
			return;
		}

		tag.forEach(into::add);
	}

	@Nullable
	private ResourceLocation resourceLocation(String id) {
		try {
			return new ResourceLocation(id);
		} catch (ResourceLocationException e) {
			SOLPotPie.LOGGER.warn("Food group {}: '{}' is not a valid id, ignoring", identifier, id);
			return null;
		}
	}

	public boolean contains(Item food) {
		return members.contains(food);
	}

	public String identifier() {
		return identifier;
	}

	public boolean enabled() {
		return enabled;
	}

	public boolean blacklist() {
		return blacklist;
	}

	public boolean hidden() {
		return hidden;
	}

	public int size() {
		return members.size();
	}

	public Set<Item> members() {
		return Collections.unmodifiableSet(members);
	}

	public MutableComponent displayName() {
		MutableComponent component = name == null
			? Component.translatable("foodgroup." + SOLPotPie.MOD_ID + "." + identifier)
			: Component.translatable(name);
		return component.withStyle(color);
	}

	@Override
	public int hashCode() {
		return identifier.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FoodGroup other && identifier.equals(other.identifier);
	}

	@Override
	public String toString() {
		return identifier;
	}
}
