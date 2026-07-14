package com.example.wrestlingmania.registry;

import com.example.wrestlingmania.WrestlingMania;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
	public static final Identifier CROWD_CHEER_ID = new Identifier(WrestlingMania.MOD_ID, "crowd_cheer");
	public static final SoundEvent CROWD_CHEER = SoundEvent.of(CROWD_CHEER_ID);

	public static void register() {
		Registry.register(Registries.SOUND_EVENT, CROWD_CHEER_ID, CROWD_CHEER);
	}
}
