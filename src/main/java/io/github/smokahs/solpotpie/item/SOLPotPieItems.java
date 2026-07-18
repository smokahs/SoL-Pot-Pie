package io.github.smokahs.solpotpie.item;

import io.github.smokahs.solpotpie.SOLPotPie;
import io.github.smokahs.solpotpie.item.foodcontainer.FoodContainerItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@Mod.EventBusSubscriber(modid = SOLPotPie.MOD_ID, bus = MOD)
public final class SOLPotPieItems
{

	@SubscribeEvent
	public static void registerItems(RegisterEvent event) {
		event.register(ForgeRegistries.Keys.ITEMS,
				helper -> {
					helper.register(new ResourceLocation(SOLPotPie.MOD_ID, "food_book"),
							new FoodBookItem());
					helper.register(new ResourceLocation(SOLPotPie.MOD_ID, "lunchbox"),
							new FoodContainerItem(9,"lunchbox"));
					helper.register(new ResourceLocation(SOLPotPie.MOD_ID, "lunchbag"),
							new FoodContainerItem(5,"lunchbag"));
					helper.register(new ResourceLocation(SOLPotPie.MOD_ID, "golden_lunchbox"),
							new FoodContainerItem(14,"golden_lunchbox"));
				});
	}
}
