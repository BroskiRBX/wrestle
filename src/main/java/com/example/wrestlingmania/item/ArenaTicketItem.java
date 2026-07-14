package com.example.wrestlingmania.item;

import com.example.wrestlingmania.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Right click the ground with this to build a full indoor wrestling arena:
 * raised ring with chain ropes, tiered seating, entrance ramp with red carpet,
 * shroomlight ceiling grid and a banner wall. The whole thing rotates to face
 * away from the player, and it refuses to build if the space is not clear.
 *
 * This uses manual block placement (no structure NBT files) so everything can
 * be tweaked in code. The rotation trick is in local(): every block is placed
 * in "arena space" (x = side to side, z = forward, y = up) and converted to
 * world space using the player's facing.
 */
public class ArenaTicketItem extends Item {
	// Local arena bounds: 20 wide, 30 long, 15 tall.
	private static final int X_MIN = -10, X_MAX = 9;
	private static final int Z_MIN = 0, Z_MAX = 29;
	private static final int Y_MIN = 0, Y_MAX = 14;

	public ArenaTicketItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		if (player == null) return ActionResult.PASS;
		// Client just swings the hand; the server does the real work.
		if (world.isClient) return ActionResult.SUCCESS;

		ServerWorld serverWorld = (ServerWorld) world;
		Direction facing = player.getHorizontalFacing();
		// Arena floor sits one block above whatever was clicked.
		BlockPos origin = context.getBlockPos().up();

		if (!hasClearance(serverWorld, origin, facing)) {
			// true = actionbar instead of chat
			player.sendMessage(Text.translatable("message.wrestling_mania.no_space"), true);
			return ActionResult.FAIL;
		}

		buildArena(serverWorld, origin, facing);

		player.sendMessage(Text.translatable("message.wrestling_mania.arena_built"), true);
		serverWorld.playSound(null, origin, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
		if (!player.getAbilities().creativeMode) {
			context.getStack().decrement(1);
		}
		return ActionResult.CONSUME;
	}

	/**
	 * Converts arena-local coordinates to a world position, rotated by facing.
	 * "forward" is the player's facing, "right" is 90 degrees clockwise from it,
	 * so the exact same offsets work for all four horizontal rotations.
	 */
	private static BlockPos local(BlockPos origin, Direction facing, int x, int y, int z) {
		Direction right = facing.rotateYClockwise();
		return origin.offset(right, x).offset(facing, z).up(y);
	}

	/** Every block in the volume must be replaceable, and the ground below must be solid. */
	private static boolean hasClearance(ServerWorld world, BlockPos origin, Direction facing) {
		for (int x = X_MIN; x <= X_MAX; x++) {
			for (int z = Z_MIN; z <= Z_MAX; z++) {
				for (int y = Y_MIN; y <= Y_MAX; y++) {
					BlockPos p = local(origin, facing, x, y, z);
					if (!world.getBlockState(p).isReplaceable()) return false;
				}
				// Spot check the ground under the footprint every 5 blocks so the
				// arena is not hanging off a cliff. Not every column, for speed.
				if ((x - X_MIN) % 5 == 0 && (z - Z_MIN) % 5 == 0) {
					BlockPos below = local(origin, facing, x, -1, z);
					if (!world.getBlockState(below).isSolidBlock(world, below)) return false;
				}
			}
		}
		return true;
	}

	private static void buildArena(ServerWorld world, BlockPos origin, Direction facing) {
		Direction right = facing.rotateYClockwise();
		BlockState air = Blocks.AIR.getDefaultState();
		BlockState floor = Blocks.GRAY_CONCRETE.getDefaultState();
		BlockState wall = Blocks.SMOOTH_STONE.getDefaultState();
		BlockState stripe = Blocks.BLACK_CONCRETE.getDefaultState();
		BlockState light = Blocks.SHROOMLIGHT.getDefaultState();
		BlockState apron = Blocks.SMOOTH_QUARTZ.getDefaultState();
		BlockState mat = Blocks.WHITE_CONCRETE.getDefaultState();
		BlockState post = Blocks.IRON_BLOCK.getDefaultState();
		BlockState platform = Blocks.GRAY_CONCRETE.getDefaultState();

		// 1) Shell: floor, ceiling with a shroomlight grid, walls with a black stripe.
		for (int x = X_MIN; x <= X_MAX; x++) {
			for (int z = Z_MIN; z <= Z_MAX; z++) {
				for (int y = Y_MIN; y <= Y_MAX; y++) {
					BlockPos p = local(origin, facing, x, y, z);
					BlockState state;
					if (y == Y_MIN) {
						state = floor;
					} else if (y == Y_MAX) {
						// Light grid pattern in the ceiling.
						state = (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 2) ? light : wall;
					} else if (x == X_MIN || x == X_MAX || z == Z_MIN || z == Z_MAX) {
						state = (y == 6) ? stripe : wall;
					} else {
						// Clear the interior of grass, snow, flowers, whatever.
						state = air;
					}
					world.setBlockState(p, state, Block.NOTIFY_LISTENERS);
				}
			}
		}

		// 2) Entrance: carve a 3 wide, 4 tall doorway in the front wall.
		for (int x = -1; x <= 1; x++) {
			for (int y = 1; y <= 4; y++) {
				world.setBlockState(local(origin, facing, x, y, Z_MIN), air, Block.NOTIFY_LISTENERS);
			}
		}

		// 3) Ring platform: 12x12, raised one block. Quartz apron, white mat inside.
		for (int x = -6; x <= 5; x++) {
			for (int z = 9; z <= 20; z++) {
				boolean edge = (x == -6 || x == 5 || z == 9 || z == 20);
				world.setBlockState(local(origin, facing, x, 1, z), edge ? apron : mat, Block.NOTIFY_LISTENERS);
			}
		}

		// 4) Corner posts with lanterns on top, chain "ropes" at two heights.
		int[][] corners = { {-6, 9}, {5, 9}, {-6, 20}, {5, 20} };
		for (int[] c : corners) {
			for (int y = 2; y <= 4; y++) {
				world.setBlockState(local(origin, facing, c[0], y, c[1]), post, Block.NOTIFY_LISTENERS);
			}
			world.setBlockState(local(origin, facing, c[0], 5, c[1]),
					Blocks.LANTERN.getDefaultState().with(Properties.HANGING, false), Block.NOTIFY_LISTENERS);
		}
		// Chains along local x need the "right" axis, chains along local z the "facing" axis,
		// otherwise the rope segments render sideways after rotation.
		BlockState chainX = Blocks.CHAIN.getDefaultState().with(Properties.AXIS, right.getAxis());
		BlockState chainZ = Blocks.CHAIN.getDefaultState().with(Properties.AXIS, facing.getAxis());
		for (int y = 2; y <= 3; y++) {
			for (int x = -5; x <= 4; x++) {
				world.setBlockState(local(origin, facing, x, y, 9), chainX, Block.NOTIFY_LISTENERS);
				world.setBlockState(local(origin, facing, x, y, 20), chainX, Block.NOTIFY_LISTENERS);
			}
			for (int z = 10; z <= 19; z++) {
				world.setBlockState(local(origin, facing, -6, y, z), chainZ, Block.NOTIFY_LISTENERS);
				world.setBlockState(local(origin, facing, 5, y, z), chainZ, Block.NOTIFY_LISTENERS);
			}
		}

		// 5) Spectator seating: two stepped rows along each long wall, stairs facing the ring.
		BlockState leftSeat = Blocks.OAK_STAIRS.getDefaultState().with(Properties.HORIZONTAL_FACING, right);
		BlockState rightSeat = Blocks.OAK_STAIRS.getDefaultState().with(Properties.HORIZONTAL_FACING, right.getOpposite());
		for (int z = 3; z <= 26; z++) {
			// Left side: front row on the floor, back row raised one block.
			world.setBlockState(local(origin, facing, -8, 1, z), leftSeat, Block.NOTIFY_LISTENERS);
			world.setBlockState(local(origin, facing, -9, 1, z), platform, Block.NOTIFY_LISTENERS);
			world.setBlockState(local(origin, facing, -9, 2, z), leftSeat, Block.NOTIFY_LISTENERS);
			// Right side, mirrored.
			world.setBlockState(local(origin, facing, 7, 1, z), rightSeat, Block.NOTIFY_LISTENERS);
			world.setBlockState(local(origin, facing, 8, 1, z), platform, Block.NOTIFY_LISTENERS);
			world.setBlockState(local(origin, facing, 8, 2, z), rightSeat, Block.NOTIFY_LISTENERS);
		}

		// 6) Entrance ramp: stairs up at the door, then a raised red carpet walkway to the apron.
		for (int z = 1; z <= 8; z++) {
			for (int x = -1; x <= 1; x++) {
				if (z == 1) {
					world.setBlockState(local(origin, facing, x, 1, z),
							Blocks.OAK_STAIRS.getDefaultState().with(Properties.HORIZONTAL_FACING, facing),
							Block.NOTIFY_LISTENERS);
				} else {
					world.setBlockState(local(origin, facing, x, 1, z), platform, Block.NOTIFY_LISTENERS);
					world.setBlockState(local(origin, facing, x, 2, z), Blocks.RED_CARPET.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}

		// 7) Scoreboard banner wall at the back, facing the entrance.
		BlockState banner = Blocks.BLACK_WALL_BANNER.getDefaultState()
				.with(Properties.HORIZONTAL_FACING, facing.getOpposite());
		for (int x = -3; x <= 3; x += 2) {
			world.setBlockState(local(origin, facing, x, 8, Z_MAX - 1), banner, Block.NOTIFY_LISTENERS);
		}

		// 8) Arena Core markers, hidden in the floor under the middle of the ring.
		// A 2x2 patch (not a single block) so the crowd system's coarse scan grid
		// can never step over it. See CrowdCheerManager.
		for (int x = 0; x <= 1; x++) {
			for (int z = 14; z <= 15; z++) {
				world.setBlockState(local(origin, facing, x, 0, z),
						ModBlocks.ARENA_CORE.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
	}
}
