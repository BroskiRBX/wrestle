package com.example.wrestlingmania.item;

import com.example.wrestlingmania.client.WrestlingGlovesRenderer;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The finisher item. Hold this in your main hand and press the move keybinds.
 * All three finisher animations live on one GeckoLib controller as
 * trigger-only animations, fired from the server in FinisherMoveHandler so
 * every nearby player sees them, not just the attacker.
 */
public class WrestlingGlovesItem extends Item implements GeoItem {
	public static final String CONTROLLER = "moves";

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

	public WrestlingGlovesItem(Settings settings) {
		super(settings);
		// Required for server side triggerAnim() calls to sync to clients.
		SingletonGeoAnimatable.registerSyncedAnimatable(this);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// The default state does nothing (PlayState.STOP). The three finishers
		// only play when triggered by name from the server.
		controllers.add(new AnimationController<>(this, CONTROLLER, 0, state -> PlayState.STOP)
				.triggerableAnim("suplex", RawAnimation.begin().thenPlay("animation.wrestling_gloves.suplex"))
				.triggerableAnim("ddt", RawAnimation.begin().thenPlay("animation.wrestling_gloves.ddt"))
				.triggerableAnim("clothesline", RawAnimation.begin().thenPlay("animation.wrestling_gloves.clothesline")));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	// GeckoLib's standard item renderer hookup for Fabric. The anonymous
	// RenderProvider is only classloaded on the client, so referencing the
	// client-only renderer inside it is safe on dedicated servers.
	@Override
	public void createRenderer(Consumer<Object> consumer) {
		consumer.accept(new RenderProvider() {
			private WrestlingGlovesRenderer renderer;

			@Override
			public BuiltinModelItemRenderer getCustomRenderer() {
				if (this.renderer == null) {
					this.renderer = new WrestlingGlovesRenderer();
				}
				return this.renderer;
			}
		});
	}

	@Override
	public Supplier<Object> getRenderProvider() {
		return this.renderProvider;
	}
}
