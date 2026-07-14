package com.example.wrestlingmania.registry;

import com.example.wrestlingmania.WrestlingMania;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
	/** Blocks placed by the arena builder that mean "this is an arena". */
	public static final TagKey<Block> ARENA_MARKER =
			TagKey.of(RegistryKeys.BLOCK, new Identifier(WrestlingMania.MOD_ID, "arena_marker"));
}
