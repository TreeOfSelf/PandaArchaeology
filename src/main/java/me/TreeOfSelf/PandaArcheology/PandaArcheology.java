package me.TreeOfSelf.PandaArcheology;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class PandaArcheology implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("pandaarcheology");
	public static DespawnedItemManager despawnedItemManager;
	private static final File CONFIG_FILE = new File("./config/PandaArchaeology.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static boolean activeForFishing;
	public static boolean activeForBrushing;
	public static boolean onlyPlayerOwned;
	public static int itemLimit;
	public static int fishingChance;
	public static int brushChance;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		ServerLifecycleEvents.AFTER_SAVE.register(this::onServerSave);
		loadOrCreateConfig();
		LOGGER.info("PandaArcheology Started!");
	}

	private void onServerSave(MinecraftServer server, boolean b, boolean b1) {
		despawnedItemManager.save();
	}

	private void onServerStop(MinecraftServer server) {
		despawnedItemManager.save();
	}

	private void onServerStart(MinecraftServer server) {
		Path worldSavePath = server.getSavePath(WorldSavePath.ROOT);
		despawnedItemManager = new DespawnedItemManager(worldSavePath, server.getRegistryManager());
	}

	private void loadOrCreateConfig() {
		if (CONFIG_FILE.exists()) {
			try (FileReader reader = new FileReader(CONFIG_FILE)) {
				JsonObject json = GSON.fromJson(reader, JsonObject.class);
				activeForFishing = json.get("activeForFishing").getAsBoolean();
				activeForBrushing = json.get("activeForBrushing").getAsBoolean();
				onlyPlayerOwned = json.get("onlyPlayerOwned").getAsBoolean();
				itemLimit = json.get("itemLimit").getAsInt();
				fishingChance = json.get("fishingChance").getAsInt();
				brushChance = json.get("brushChance").getAsInt();
				LOGGER.info("Loaded configuration from file.");
			} catch (IOException e) {
				LOGGER.error("Failed to read the configuration file.", e);
				createDefaultConfig();
			}
		} else {
			createDefaultConfig();
		}
	}

	private void createDefaultConfig() {
		activeForFishing = true;
		activeForBrushing = true;
		onlyPlayerOwned = false;
		itemLimit = 10;
		fishingChance = 100;
		brushChance = 10;

		JsonObject json = new JsonObject();
		json.addProperty("activeForFishing", activeForFishing);
		json.addProperty("activeForBrushing", activeForBrushing);
		json.addProperty("onlyPlayerOwned", onlyPlayerOwned);
		json.addProperty("itemLimit", itemLimit);
		json.addProperty("fishingChance", fishingChance);
		json.addProperty("brushChance", brushChance);

		CONFIG_FILE.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
			GSON.toJson(json, writer);
			LOGGER.info("Created default configuration file.");
		} catch (IOException e) {
			LOGGER.error("Failed to write the configuration file.", e);
		}
	}


}