package com.example.wrestlingmania.config;

import com.example.wrestlingmania.WrestlingMania;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tiny JSON config written to config/wrestling_mania.json on first launch. */
public class WrestlingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static WrestlingConfig INSTANCE = new WrestlingConfig();

	// Number of oak planks a table drops when it shatters.
	public int tablePlankDrops = 4;
	// Minimum entity speed (blocks per tick) that counts as being thrown through a table.
	public double tableBreakVelocity = 0.6;
	// How often (in ticks) the crowd system checks players and plays cheers. 100 = 5 seconds.
	public int crowdIntervalTicks = 100;
	// Crowd volume at point blank range. Falls off toward the edge of the 20 block radius.
	public float crowdBaseVolume = 1.0f;

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("wrestling_mania.json");
		try {
			if (Files.exists(path)) {
				INSTANCE = GSON.fromJson(Files.readString(path), WrestlingConfig.class);
			} else {
				Files.writeString(path, GSON.toJson(INSTANCE));
			}
		} catch (IOException e) {
			WrestlingMania.LOGGER.error("Could not load config, using defaults", e);
		}
		if (INSTANCE == null) INSTANCE = new WrestlingConfig();
		// Guard rails so a bad config value cannot flood the tick loop.
		if (INSTANCE.crowdIntervalTicks < 20) INSTANCE.crowdIntervalTicks = 20;
		if (INSTANCE.tablePlankDrops < 0) INSTANCE.tablePlankDrops = 0;
	}
}
