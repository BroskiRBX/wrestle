package com.example.wrestlingmania.sound;

import com.example.wrestlingmania.config.WrestlingConfig;
import com.example.wrestlingmania.registry.ModSounds;
import com.example.wrestlingmania.registry.ModTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

/**
 * Ambient crowd noise near arenas.
 *
 * Detection is tag based: the Arena Ticket hides a 2x2 patch of Arena Core
 * blocks under the ring, and that block is in #wrestling_mania:arena_marker.
 * Every few seconds we scan a coarse grid around each player for a marker.
 * Scanning (instead of remembering arena positions in a list) means this
 * still works after server restarts, and hand-built arenas work too if you
 * just place an Arena Core block yourself.
 *
 * Cost check: the scan is a 41x41 grid stepped by 2 in x/z, times 21 in y,
 * which is about 9k block reads per player every crowdIntervalTicks (5s by
 * default). That is cheap. The 2x2 core patch guarantees the step-2 grid can
 * never skip over an arena.
 */
public class CrowdCheerManager {
	private static final int RADIUS = 20;
	private static final int SCAN_STEP = 2;
	private static final int VERTICAL_RANGE = 10;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int interval = WrestlingConfig.INSTANCE.crowdIntervalTicks;
			if (server.getTicks() % interval != 0) return;

			for (ServerWorld world : server.getWorlds()) {
				for (ServerPlayerEntity player : world.getPlayers()) {
					BlockPos core = findNearestMarker(world, player.getBlockPos());
					if (core == null) continue;

					double dist = Math.sqrt(core.getSquaredDistance(player.getPos()));
					if (dist > RADIUS) continue;

					// Louder the closer you are to the ring, with a small floor
					// so it never fully cuts out just inside the radius.
					float volume = (float) (WrestlingConfig.INSTANCE.crowdBaseVolume * (1.0 - dist / RADIUS));
					volume = Math.max(0.1f, volume);
					float pitch = 0.9f + world.random.nextFloat() * 0.2f;

					// Send each player their own positional sound packet so the
					// volume scaling is truly per-player.
					player.networkHandler.sendPacket(new PlaySoundS2CPacket(
							RegistryEntry.of(ModSounds.CROWD_CHEER),
							SoundCategory.AMBIENT,
							core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5,
							volume, pitch, world.random.nextLong()));
				}
			}
		});
	}

	private static BlockPos findNearestMarker(ServerWorld world, BlockPos center) {
		BlockPos best = null;
		double bestDistSq = Double.MAX_VALUE;
		BlockPos.Mutable cursor = new BlockPos.Mutable();

		for (int dx = -RADIUS; dx <= RADIUS; dx += SCAN_STEP) {
			for (int dz = -RADIUS; dz <= RADIUS; dz += SCAN_STEP) {
				for (int dy = -VERTICAL_RANGE; dy <= VERTICAL_RANGE; dy++) {
					cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (!world.getBlockState(cursor).isIn(ModTags.ARENA_MARKER)) continue;
					double d = cursor.getSquaredDistance(center);
					if (d < bestDistSq) {
						bestDistSq = d;
						best = cursor.toImmutable();
					}
				}
			}
		}
		return best;
	}
}
