package com.example.wrestlingmania;

import com.example.wrestlingmania.client.ClientCooldowns;
import com.example.wrestlingmania.client.FinisherHudOverlay;
import com.example.wrestlingmania.combat.FinisherMove;
import com.example.wrestlingmania.network.ModPackets;
import com.example.wrestlingmania.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

public class WrestlingManiaClient implements ClientModInitializer {
	public static KeyBinding suplexKey;
	public static KeyBinding ddtKey;
	public static KeyBinding clotheslineKey;

	@Override
	public void onInitializeClient() {
		suplexKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.wrestling_mania.suplex", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.category.wrestling_mania"));
		ddtKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.wrestling_mania.ddt", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.category.wrestling_mania"));
		clotheslineKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.wrestling_mania.clothesline", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.category.wrestling_mania"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (suplexKey.wasPressed()) requestMove(client, FinisherMove.SUPLEX);
			while (ddtKey.wasPressed()) requestMove(client, FinisherMove.DDT);
			while (clotheslineKey.wasPressed()) requestMove(client, FinisherMove.CLOTHESLINE);
		});

		// Server tells us when a cooldown starts so the HUD can count it down.
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.FINISHER_COOLDOWN_S2C, (client, handler, buf, responseSender) -> {
			int moveId = buf.readVarInt();
			int ticks = buf.readVarInt();
			client.execute(() -> ClientCooldowns.start(moveId, ticks));
		});

		HudRenderCallback.EVENT.register(new FinisherHudOverlay());
	}

	private static void requestMove(MinecraftClient client, FinisherMove move) {
		if (client.player == null) return;
		// Light client side gate: only bother the server if we are holding the
		// gloves and the HUD says the move is ready. The server re-validates
		// everything anyway, this just avoids useless packets.
		if (!client.player.getMainHandStack().isOf(ModItems.WRESTLING_GLOVES)) return;
		if (ClientCooldowns.isCoolingDown(move.id)) return;

		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(move.id);
		ClientPlayNetworking.send(ModPackets.FINISHER_MOVE_C2S, buf);
	}
}
