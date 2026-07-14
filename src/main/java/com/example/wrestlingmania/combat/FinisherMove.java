package com.example.wrestlingmania.combat;

/** The three finishers. IDs are what goes over the network. */
public enum FinisherMove {
	SUPLEX(0, "suplex", 160, 6.0f),
	DDT(1, "ddt", 240, 8.0f),
	CLOTHESLINE(2, "clothesline", 120, 5.0f);

	public final int id;
	/** Matches the .triggerableAnim names registered on the gloves. */
	public final String animTrigger;
	public final int cooldownTicks;
	public final float damage;

	FinisherMove(int id, String animTrigger, int cooldownTicks, float damage) {
		this.id = id;
		this.animTrigger = animTrigger;
		this.cooldownTicks = cooldownTicks;
		this.damage = damage;
	}

	public static FinisherMove byId(int id) {
		for (FinisherMove m : values()) {
			if (m.id == id) return m;
		}
		return null;
	}
}
