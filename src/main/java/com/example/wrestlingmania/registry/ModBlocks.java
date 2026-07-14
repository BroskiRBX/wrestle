package com.example.wrestlingmania.registry;

import com.example.wrestlingmania.WrestlingMania;
import com.example.wrestlingmania.block.BreakableTableBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
	// High hardness on purpose: the table should shatter through the punch
	// system, not through normal mining.
	public static final Block BREAKABLE_TABLE = new BreakableTableBlock(
			FabricBlockSettings.copyOf(Blocks.OAK_PLANKS).strength(8.0f, 2.0f).nonOpaque());

	// Marker block the Arena Ticket hides under the ring. The crowd system
	// finds arenas by looking for blocks in #wrestling_mania:arena_marker,
	// which contains this block (see data/wrestling_mania/tags/blocks).
	public static final Block ARENA_CORE = new Block(
			FabricBlockSettings.copyOf(Blocks.NOTE_BLOCK).strength(3.0f));

	public static void register() {
		registerWithItem("breakable_table", BREAKABLE_TABLE);
		registerWithItem("arena_core", ARENA_CORE);
	}

	private static void registerWithItem(String path, Block block) {
		Identifier id = new Identifier(WrestlingMania.MOD_ID, path);
		Registry.register(Registries.BLOCK, id, block);
		Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
	}
}
