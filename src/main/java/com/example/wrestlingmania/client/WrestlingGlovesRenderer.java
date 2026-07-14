package com.example.wrestlingmania.client;

import com.example.wrestlingmania.item.WrestlingGlovesItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class WrestlingGlovesRenderer extends GeoItemRenderer<WrestlingGlovesItem> {
	public WrestlingGlovesRenderer() {
		super(new WrestlingGlovesModel());
	}
}
