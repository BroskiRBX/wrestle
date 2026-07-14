package com.example.wrestlingmania.registry;

import com.example.wrestlingmania.WrestlingMania;
import com.example.wrestlingmania.item.ArenaTicketItem;
import com.example.wrestlingmania.item.WrestlingGlovesItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
	public static final Item ARENA_TICKET = new ArenaTicketItem(new FabricItemSettings().maxCount(16));
	public static final Item WRESTLING_GLOVES = new WrestlingGlovesItem(new FabricItemSettings().maxCount(1));

	public static void register() {
		Registry.register(Registries.ITEM, id("arena_ticket"), ARENA_TICKET);
		Registry.register(Registries.ITEM, id("wrestling_gloves"), WRESTLING_GLOVES);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
			entries.add(ARENA_TICKET);
			entries.add(WRESTLING_GLOVES);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
			entries.add(ModBlocks.BREAKABLE_TABLE.asItem());
			entries.add(ModBlocks.ARENA_CORE.asItem());
		});
	}

	private static Identifier id(String path) {
		return new Identifier(WrestlingMania.MOD_ID, path);
	}
}
