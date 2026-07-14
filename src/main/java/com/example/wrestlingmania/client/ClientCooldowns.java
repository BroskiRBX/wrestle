package com.example.wrestlingmania.client;

import com.example.wrestlingmania.combat.FinisherMove;
import net.minecraft.client.MinecraftClient;

/** Client side mirror of the server cooldowns. Only exists to feed the HUD. */
public class ClientCooldowns {
	private static final long[] END_TIMES = new long[FinisherMove.values().length];
	private static final int[] DURATIONS = new int[FinisherMove.values().length];

	public static void start(int moveId, int ticks) {
		if (moveId < 0 || moveId >= END_TIMES.length) return;
		END_TIMES[moveId] = worldTime() + ticks;
		DURATIONS[moveId] = ticks;
	}

	public static boolean isCoolingDown(int moveId) {
		return remainingTicks(moveId) > 0;
	}

	public static long remainingTicks(int moveId) {
		return Math.max(0, END_TIMES[moveId] - worldTime());
	}

	/** 0.0 = just used, 1.0 = fully ready. */
	public static float readiness(int moveId) {
		if (DURATIONS[moveId] <= 0) return 1.0f;
		long remaining = remainingTicks(moveId);
		if (remaining <= 0) return 1.0f;
		return 1.0f - (float) remaining / DURATIONS[moveId];
	}

	private static long worldTime() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.world == null ? 0 : client.world.getTime();
	}
}
