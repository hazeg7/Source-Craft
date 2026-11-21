package com.hazeg7.sourcecraft.block;

import com.hazeg7.sourcecraft.SourceCraftMod;
import com.hazeg7.sourcecraft.item.MapWandItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registers SourceCraft's custom surf ramps, boost pads, KZ/Surf timer pads, Map Wand, and creative tab.
 */
public final class SourceBlocks {
	public static final Block SURF_RAMP_30 = registerBlock("surf_ramp_30",
		new SurfRampBlock(30, metalRampProps("surf_ramp_30")));
	public static final Block SURF_RAMP_45 = registerBlock("surf_ramp_45",
		new SurfRampBlock(45, metalRampProps("surf_ramp_45")));
	public static final Block SURF_RAMP_60 = registerBlock("surf_ramp_60",
		new SurfRampBlock(60, metalRampProps("surf_ramp_60")));

	public static final Block BOOST_PAD = registerBlock("boost_pad",
		new BoostPadBlock(padProps("boost_pad")));
	public static final Block TIMER_START_PAD = registerBlock("timer_start_pad",
		new TimerPadBlock(TimerPadBlock.PadKind.START, padProps("timer_start_pad")));
	public static final Block TIMER_END_PAD = registerBlock("timer_end_pad",
		new TimerPadBlock(TimerPadBlock.PadKind.END, padProps("timer_end_pad")));
	public static final Block CHECKPOINT_PAD = registerBlock("checkpoint_pad",
		new TimerPadBlock(TimerPadBlock.PadKind.CHECKPOINT, padProps("checkpoint_pad")));

	public static final Item SURF_RAMP_30_ITEM = registerBlockItem("surf_ramp_30", SURF_RAMP_30);
	public static final Item SURF_RAMP_45_ITEM = registerBlockItem("surf_ramp_45", SURF_RAMP_45);
	public static final Item SURF_RAMP_60_ITEM = registerBlockItem("surf_ramp_60", SURF_RAMP_60);
	public static final Item BOOST_PAD_ITEM = registerBlockItem("boost_pad", BOOST_PAD);
	public static final Item TIMER_START_PAD_ITEM = registerBlockItem("timer_start_pad", TIMER_START_PAD);
	public static final Item TIMER_END_PAD_ITEM = registerBlockItem("timer_end_pad", TIMER_END_PAD);
	public static final Item CHECKPOINT_PAD_ITEM = registerBlockItem("checkpoint_pad", CHECKPOINT_PAD);

	public static final Item MAP_WAND = registerItem("map_wand",
		new MapWandItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, SourceCraftMod.id("map_wand")))));

	public static final CreativeModeTab SOURCECRAFT_TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		SourceCraftMod.id("sourcecraft_tab"),
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.sourcecraft"))
			.icon(() -> new ItemStack(SURF_RAMP_45_ITEM))
			.displayItems((params, output) -> {
				output.accept(SURF_RAMP_30_ITEM);
				output.accept(SURF_RAMP_45_ITEM);
				output.accept(SURF_RAMP_60_ITEM);
				output.accept(BOOST_PAD_ITEM);
				output.accept(TIMER_START_PAD_ITEM);
				output.accept(TIMER_END_PAD_ITEM);
				output.accept(CHECKPOINT_PAD_ITEM);
				output.accept(MAP_WAND);
			})
			.build()
	);

	private SourceBlocks() {
	}

	private static BlockBehaviour.Properties metalRampProps(String path) {
		return BlockBehaviour.Properties.of()
			.strength(2.0f, 6.0f)
			.noOcclusion()
			.sound(SoundType.METAL)
			.setId(ResourceKey.create(Registries.BLOCK, SourceCraftMod.id(path)));
	}

	private static BlockBehaviour.Properties padProps(String path) {
		return BlockBehaviour.Properties.of()
			.strength(1.5f, 6.0f)
			.noOcclusion()
			.sound(SoundType.COPPER)
			.setId(ResourceKey.create(Registries.BLOCK, SourceCraftMod.id(path)));
	}

	private static Block registerBlock(String path, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, SourceCraftMod.id(path), block);
	}

	private static Item registerBlockItem(String path, Block block) {
		return Registry.register(
			BuiltInRegistries.ITEM,
			SourceCraftMod.id(path),
			new BlockItem(block, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SourceCraftMod.id(path))))
		);
	}

	private static Item registerItem(String path, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, SourceCraftMod.id(path), item);
	}

	public static void init() {
		// Triggers static registration of all custom SourceCraft blocks, items, and tab.
	}
}
