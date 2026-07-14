package com.example.wrestlingmania;

import com.example.wrestlingmania.block.BreakableTableBlock;
import com.example.wrestlingmania.combat.FinisherMoveHandler;
import com.example.wrestlingmania.config.WrestlingConfig;
import com.example.wrestlingmania.network.ModPackets;
import com.example.wrestlingmania.registry.ModBlockEntities;
import com.example.wrestlingmania.registry.ModBlocks;
import com.example.wrestlingmania.registry.ModItems;
import com.example.wrestlingmania.registry.ModSounds;
import com.example.wrestlingmania.sound.CrowdCheerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrestlingMania implements ModInitializer {
	public static final String MOD_ID = "wrestling_mania";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		WrestlingConfig.load();

		ModSounds.register();
		ModBlocks.register();
		ModBlockEntities.register();
		ModItems.register();

		ModPackets.registerC2SPackets();
		FinisherMoveHandler.init();
		CrowdCheerManager.init();

		// Table punch system. We intercept left clicks on the table so vanilla
		// mining can never bypass the 3-hit health system. Returning SUCCESS
		// cancels the vanilla block-breaking flow.
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			BlockState state = world.getBlockState(pos);
			if (state.isOf(ModBlocks.BREAKABLE_TABLE)) {
				if (!world.isClient) {
					BreakableTableBlock.punch((ServerWorld) world, pos, player);
				}
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});

		LOGGER.info("Wrestling Mania loaded. Cue the pyro.");
	}
}
