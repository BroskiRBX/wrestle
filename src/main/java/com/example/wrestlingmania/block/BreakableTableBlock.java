package com.example.wrestlingmania.block;

import com.example.wrestlingmania.config.WrestlingConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A wooden table with 3 hit points. Punch it (left click) three times and it
 * shatters with a splinter burst and drops planks. Fast moving entities that
 * end up inside its open space (thrown through it) break it instantly, and so
 * does landing hard on the tabletop.
 */
public class BreakableTableBlock extends BlockWithEntity {
	// Tabletop plus four legs, matching the block model JSON.
	private static final VoxelShape SHAPE = VoxelShapes.union(
			Block.createCuboidShape(0, 12, 0, 16, 16, 16),
			Block.createCuboidShape(1, 0, 1, 3, 12, 3),
			Block.createCuboidShape(13, 0, 1, 15, 12, 3),
			Block.createCuboidShape(1, 0, 13, 3, 12, 15),
			Block.createCuboidShape(13, 0, 13, 15, 12, 15));

	public BreakableTableBlock(Settings settings) {
		super(settings);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new BreakableTableBlockEntity(pos, state);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		// BlockWithEntity defaults to INVISIBLE, we want the JSON model.
		return BlockRenderType.MODEL;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	/** Called from the AttackBlockCallback in the main initializer. One left click = one hit. */
	public static void punch(ServerWorld world, BlockPos pos, PlayerEntity player) {
		if (!(world.getBlockEntity(pos) instanceof BreakableTableBlockEntity table)) return;

		if (table.takeHit()) {
			shatter(world, pos);
		} else {
			// Crack feedback for the first two hits.
			BlockState state = world.getBlockState(pos);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
					pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
					12, 0.3, 0.1, 0.3, 0.05);
			world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 1.0f,
					0.7f + world.random.nextFloat() * 0.3f);
		}
	}

	/** The full table spot: loud break, big splinter burst, configurable plank drops. */
	public static void shatter(ServerWorld world, BlockPos pos) {
		BlockState planks = Blocks.OAK_PLANKS.getDefaultState();
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, planks),
				pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
				60, 0.5, 0.4, 0.5, 0.2);
		world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.6f, 0.75f);
		world.playSound(null, pos, SoundEvents.BLOCK_BAMBOO_WOOD_BREAK, SoundCategory.BLOCKS, 1.2f, 0.6f);

		// Remove with no vanilla drops, then drop the configured plank count ourselves.
		world.breakBlock(pos, false);
		int drops = WrestlingConfig.INSTANCE.tablePlankDrops;
		if (drops > 0) {
			ItemEntity item = new ItemEntity(world,
					pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					new ItemStack(Items.OAK_PLANKS, drops));
			world.spawnEntity(item);
		}
	}

	/**
	 * "Thrown through the table" detection. There is open space between the legs
	 * and under the top, so a fast moving entity (a suplexed player, for example)
	 * can end up inside this block's space, which fires this hook. Fast enough
	 * and the table instantly breaks, no matter how many hits it had left.
	 */
	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (world.isClient || !(entity instanceof LivingEntity)) return;
		Vec3d v = entity.getVelocity();
		double threshold = WrestlingConfig.INSTANCE.tableBreakVelocity;
		if (v.lengthSquared() >= threshold * threshold) {
			shatter((ServerWorld) world, pos);
		}
	}

	/** Landing hard on the tabletop also breaks it. Classic. */
	@Override
	public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
		if (!world.isClient && entity instanceof LivingEntity && fallDistance > 1.5f) {
			shatter((ServerWorld) world, pos);
			// The table absorbed most of the impact, so soften the fall.
			entity.handleFallDamage(fallDistance, 0.3f, world.getDamageSources().fall());
			return;
		}
		super.onLandedUpon(world, state, pos, entity, fallDistance);
	}
}
