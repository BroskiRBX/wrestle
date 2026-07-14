package com.example.wrestlingmania.network;

import com.example.wrestlingmania.WrestlingMania;
import com.example.wrestlingmania.combat.FinisherMoveHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModPackets {
	// C2S: client asks to perform finisher <id>. The server validates everything.
	public static final Identifier FINISHER_MOVE_C2S = new Identifier(WrestlingMania.MOD_ID, "finisher_move");
	// S2C: server tells one client "move <id> is on cooldown for <ticks>" for the HUD.
	public static final Identifier FINISHER_COOLDOWN_S2C = new Identifier(WrestlingMania.MOD_ID, "finisher_cooldown");

	public static void registerC2SPackets() {
		ServerPlayNetworking.registerGlobalReceiver(FINISHER_MOVE_C2S, (server, player, handler, buf, responseSender) -> {
			int moveId = buf.readVarInt();
			// Networking callbacks run off-thread. Hop onto the server thread
			// before touching the world or entities.
			server.execute(() -> FinisherMoveHandler.tryExecute(player, moveId));
		});
	}

	public static void sendCooldown(ServerPlayerEntity player, int moveId, int ticks) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(moveId);
		buf.writeVarInt(ticks);
		ServerPlayNetworking.send(player, FINISHER_COOLDOWN_S2C, buf);
	}
}
