package com.example.wrestlingmania.client;

import com.example.wrestlingmania.WrestlingMania;
import com.example.wrestlingmania.item.WrestlingGlovesItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/** Points GeckoLib at the gloves geometry, texture and animation files. */
public class WrestlingGlovesModel extends GeoModel<WrestlingGlovesItem> {
	@Override
	public Identifier getModelResource(WrestlingGlovesItem animatable) {
		return new Identifier(WrestlingMania.MOD_ID, "geo/wrestling_gloves.geo.json");
	}

	@Override
	public Identifier getTextureResource(WrestlingGlovesItem animatable) {
		return new Identifier(WrestlingMania.MOD_ID, "textures/item/wrestling_gloves_geo.png");
	}

	@Override
	public Identifier getAnimationResource(WrestlingGlovesItem animatable) {
		return new Identifier(WrestlingMania.MOD_ID, "animations/wrestling_gloves.animation.json");
	}
}
