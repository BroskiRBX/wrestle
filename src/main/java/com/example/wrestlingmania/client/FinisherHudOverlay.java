package com.example.wrestlingmania.client;

import com.example.wrestlingmania.combat.FinisherMove;
import com.example.wrestlingmania.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Small cooldown panel on the left edge of the screen, only visible while the
 * Wrestling Gloves are in the main hand. Each move gets a bar that refills as
 * its cooldown recovers: red while cooling down, green when ready.
 */
public class FinisherHudOverlay implements HudRenderCallback {
	private static final int BOX_WIDTH = 74;
	private static final int BOX_HEIGHT = 14;
	private static final int GAP = 3;

	@Override
	public void onHudRender(DrawContext ctx, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) return;
		if (!client.player.getMainHandStack().isOf(ModItems.WRESTLING_GLOVES)) return;

		FinisherMove[] moves = FinisherMove.values();
		int x = 6;
		int totalHeight = moves.length * (BOX_HEIGHT + GAP) - GAP;
		int y = ctx.getScaledWindowHeight() / 2 - totalHeight / 2;

		for (FinisherMove move : moves) {
			float readiness = ClientCooldowns.readiness(move.id);
			boolean ready = readiness >= 1.0f;

			// Dark background, then a fill bar that grows as the cooldown recovers.
			ctx.fill(x, y, x + BOX_WIDTH, y + BOX_HEIGHT, 0xA0101010);
			int fillWidth = (int) (BOX_WIDTH * readiness);
			ctx.fill(x, y, x + fillWidth, y + BOX_HEIGHT, ready ? 0x8032CD32 : 0x80CD3232);

			MutableText label = Text.translatable("move.wrestling_mania." + move.animTrigger);
			if (!ready) {
				long secs = (ClientCooldowns.remainingTicks(move.id) + 19) / 20;
				label = label.append(Text.literal(" " + secs + "s"));
			}
			ctx.drawText(client.textRenderer, label, x + 4, y + 3, 0xFFFFFF, true);

			y += BOX_HEIGHT + GAP;
		}
	}
}
