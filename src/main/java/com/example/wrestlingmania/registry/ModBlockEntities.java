package com.example.wrestlingmania.registry;

import com.example.wrestlingmania.WrestlingMania;
import com.example.wrestlingmania.block.BreakableTableBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
	public static BlockEntityType<BreakableTableBlockEntity> BREAKABLE_TABLE;

	public static void register() {
		BREAKABLE_TABLE = Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				new Identifier(WrestlingMania.MOD_ID, "breakable_table"),
				FabricBlockEntityTypeBuilder.create(BreakableTableBlockEntity::new, ModBlocks.BREAKABLE_TABLE).build());
	}
}
