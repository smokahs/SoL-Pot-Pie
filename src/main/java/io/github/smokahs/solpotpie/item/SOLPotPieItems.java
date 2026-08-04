package io.github.smokahs.solpotpie.item;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.SOLPotPieConfig;
import io.github.smokahs.solpotpie.item.foodcontainer.FoodContainerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SOLPotPieItems
{
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, SOLPotPie.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SOLPotPie.MOD_ID);

	public static final RegistryObject<Item> FOOD_BOOK =
			ITEMS.register("food_book", FoodBookItem::new);
	public static final RegistryObject<Item> LUNCHBOX =
			ITEMS.register("lunchbox", () -> new FoodContainerItem(SOLPotPieConfig::lunchboxSlots, "lunchbox"));
	public static final RegistryObject<Item> LUNCHBAG =
			ITEMS.register("lunchbag", () -> new FoodContainerItem(SOLPotPieConfig::lunchbagSlots, "lunchbag"));
	public static final RegistryObject<Item> GOLDEN_LUNCHBOX =
			ITEMS.register("golden_lunchbox", () -> new FoodContainerItem(SOLPotPieConfig::goldenLunchboxSlots, "golden_lunchbox"));
	public static final RegistryObject<Item> POT_PIE =
			ITEMS.register("pot_pie", () -> new Item(new Item.Properties()
					.stacksTo(16)
					.food(new FoodProperties.Builder().nutrition(10).saturationMod(0.8F).build())));

	public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("solpotpie",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup." + SOLPotPie.MOD_ID))
					.icon(() -> new ItemStack(LUNCHBOX.get()))
					.displayItems((parameters, output) -> {
						output.accept(FOOD_BOOK.get());
						output.accept(LUNCHBAG.get());
						output.accept(LUNCHBOX.get());
						output.accept(GOLDEN_LUNCHBOX.get());
						output.accept(POT_PIE.get());
					})
					.build());

	public static void register(IEventBus modEventBus) {
		ITEMS.register(modEventBus);
		CREATIVE_MODE_TABS.register(modEventBus);
	}

	private SOLPotPieItems() {}
}
