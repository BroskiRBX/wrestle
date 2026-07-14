package com.example.wrestlingmania.combat;

import com.example.wrestlingmania.item.WrestlingGlovesItem;
import com.example.wrestlingmania.network.ModPackets;
import com.example.wrestlingmania.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side brain for the finisher system. The client only ever sends
 * "I pressed the button for move X". Target selection, cooldowns, gloves
 * checks, damage and knockback all happen here, so a hacked client cannot
 * skip cooldowns or hit things it should not be able to hit.
 */
public class FinisherMoveHandler {
	// Per player cooldown end times in server ticks, indexed by FinisherMove.id.
	private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();
	// Tiny delayed-task queue, used for the DDT's lift-then-slam timing.
	private static final List<ScheduledTask> TASKS = new ArrayList<>();

	private record ScheduledTask(long runAtTick, Runnable action) {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (TASKS.isEmpty()) return;
			long now = server.getTicks();
			Iterator<ScheduledTask> it = TASKS.iterator();
			while (it.hasNext()) {
				ScheduledTask task = it.next();
				if (now >= task.runAtTick()) {
					it.remove();
					task.action().run();
				}
			}
		});
		// Do not leak state between worlds / server restarts in singleplayer.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TASKS.clear();
			COOLDOWNS.clear();
		});
	}

	private static void schedule(MinecraftServer server, int delayTicks, Runnable action) {
		TASKS.add(new ScheduledTask(server.getTicks() + delayTicks, action));
	}

	/** Entry point from the C2S packet. Runs on the server thread. */
	public static void tryExecute(ServerPlayerEntity player, int moveId) {
		FinisherMove move = FinisherMove.byId(moveId);
		if (move == null) return;

		// Rule 1: gloves in the main hand. The client checks too, but never trust the client.
		ItemStack stack = player.getMainHandStack();
		if (!stack.isOf(ModItems.WRESTLING_GLOVES)) {
			player.sendMessage(Text.translatable("message.wrestling_mania.need_gloves"), true);
			return;
		}

		// Rule 2: cooldown check.
		ServerWorld world = player.getServerWorld();
		long now = world.getServer().getTicks();
		long[] ends = COOLDOWNS.computeIfAbsent(player.getUuid(), u -> new long[FinisherMove.values().length]);
		if (now < ends[move.id]) {
			player.sendMessage(Text.translatable("message.wrestling_mania.on_cooldown"), true);
			return;
		}

		// Rule 3: need a living target roughly on the crosshair, within 4 blocks.
		LivingEntity target = findTarget(player);
		if (target == null) {
			player.sendMessage(Text.translatable("message.wrestling_mania.no_target"), true);
			return;
		}

		// Fire the GeckoLib animation on the gloves so everyone nearby sees it.
		if (stack.getItem() instanceof WrestlingGlovesItem gloves) {
			long animId = GeoItem.getOrAssignId(stack, world);
			gloves.triggerAnim(player, animId, WrestlingGlovesItem.CONTROLLER, move.animTrigger);
		}

		applyMove(player, target, move, world);

		// Start the cooldown and tell the client so the HUD can count it down.
		ends[move.id] = now + move.cooldownTicks;
		ModPackets.sendCooldown(player, move.id, move.cooldownTicks);
	}

	private static void applyMove(ServerPlayerEntity player, LivingEntity target, FinisherMove move, ServerWorld world) {
		target.damage(player.getDamageSources().playerAttack(player), move.damage);

		switch (move) {
			case SUPLEX -> {
				// Launch straight up with a short daze.
				target.setVelocity(target.getVelocity().x, 1.15, target.getVelocity().z);
				// This flag makes the server actually send the velocity packet to clients.
				target.velocityModified = true;
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 1));
				world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
						SoundCategory.PLAYERS, 1.0f, 0.8f);
			}
			case DDT -> {
				// Lift for half a second, then cut the levitation and spike them down.
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 12, 3));
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0));
				schedule(world.getServer(), 10, () -> {
					if (!target.isAlive()) return;
					target.removeStatusEffect(StatusEffects.LEVITATION);
					target.setVelocity(target.getVelocity().x, -1.6, target.getVelocity().z);
					target.velocityModified = true;
					world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_BIG_FALL,
							SoundCategory.PLAYERS, 1.0f, 0.7f);
				});
			}
			case CLOTHESLINE -> {
				// Big horizontal knockback in the direction the attacker is looking.
				Vec3d look = player.getRotationVec(1.0f);
				// takeKnockback pushes AWAY from the given x/z, so pass the reversed look.
				target.takeKnockback(1.6, -look.x, -look.z);
				target.velocityModified = true;
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 30, 0));
				world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
						SoundCategory.PLAYERS, 1.0f, 0.9f);
			}
		}

		// Impact sparks so the hit reads visually for everyone.
		world.spawnParticles(ParticleTypes.CRIT,
				target.getX(), target.getBodyY(0.6), target.getZ(),
				15, 0.4, 0.4, 0.4, 0.3);
	}

	/**
	 * Picks the living entity closest to the player's crosshair within 4 blocks.
	 * We do our own scan instead of trusting an entity id from the client.
	 */
	private static LivingEntity findTarget(ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0f);
		Box searchBox = player.getBoundingBox().stretch(look.multiply(4.0)).expand(1.0);
		List<LivingEntity> candidates = player.getWorld().getEntitiesByClass(LivingEntity.class, searchBox,
				e -> e != player && e.isAlive() && !e.isSpectator() && player.canSee(e));

		LivingEntity best = null;
		double bestDot = 0.35; // must be at least vaguely in front of the player
		for (LivingEntity e : candidates) {
			Vec3d toTarget = e.getPos().add(0, e.getHeight() / 2.0, 0).subtract(player.getEyePos());
			if (toTarget.length() > 4.0) continue;
			double dot = toTarget.normalize().dotProduct(look);
			if (dot > bestDot) {
				bestDot = dot;
				best = e;
			}
		}
		return best;
	}
}
