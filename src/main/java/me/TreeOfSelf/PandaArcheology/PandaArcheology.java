package me.TreeOfSelf.PandaArcheology;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.List;


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
	public static int luckMultiplier;
	public static boolean globalBroadcast;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		ServerLifecycleEvents.AFTER_SAVE.register(this::onServerSave);
		loadOrCreateConfig();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(
						Commands.literal("lostloot")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.literal("list")
										.executes(PandaArcheology::listPool))
				)
		);
		LOGGER.info("PandaArcheology Started!");
	}

	private void onServerSave(MinecraftServer server, boolean b, boolean b1) {
		despawnedItemManager.save();
	}

	private void onServerStop(MinecraftServer server) {
		despawnedItemManager.save();
	}

	private void onServerStart(MinecraftServer server) {
		Path worldSavePath = server.getWorldPath(LevelResource.ROOT);
		despawnedItemManager = new DespawnedItemManager(worldSavePath, server.registryAccess());
	}

	private void loadOrCreateConfig() {
		boolean needsUpdate = false;

		if (CONFIG_FILE.exists()) {
			try (FileReader reader = new FileReader(CONFIG_FILE)) {
				JsonObject json = GSON.fromJson(reader, JsonObject.class);

				if (json.has("activeForFishing")) {
					activeForFishing = json.get("activeForFishing").getAsBoolean();
				} else {
					activeForFishing = true;
					needsUpdate = true;
				}

				if (json.has("activeForBrushing")) {
					activeForBrushing = json.get("activeForBrushing").getAsBoolean();
				} else {
					activeForBrushing = true;
					needsUpdate = true;
				}

				if (json.has("onlyPlayerOwned")) {
					onlyPlayerOwned = json.get("onlyPlayerOwned").getAsBoolean();
				} else {
					onlyPlayerOwned = false;
					needsUpdate = true;
				}

				if (json.has("itemLimit")) {
					itemLimit = json.get("itemLimit").getAsInt();
				} else {
					itemLimit = 10;
					needsUpdate = true;
				}

				if (json.has("fishingChance")) {
					fishingChance = Math.max(1, json.get("fishingChance").getAsInt());
				} else {
					fishingChance = 100;
					needsUpdate = true;
				}

				if (json.has("brushChance")) {
					brushChance = Math.max(1, json.get("brushChance").getAsInt());
				} else {
					brushChance = 10;
					needsUpdate = true;
				}

				if (json.has("luckMultiplier")) {
					luckMultiplier = json.get("luckMultiplier").getAsInt();
				} else {
					luckMultiplier = 3;
					needsUpdate = true;
				}
				if (json.has("global_broadcast")) {
					globalBroadcast = json.get("global_broadcast").getAsBoolean();
				} else {
					globalBroadcast = false;
					needsUpdate = true;
				}

				if (needsUpdate) {
					saveConfig();
					LOGGER.info("Updated configuration file with missing values.");
				} else {
					LOGGER.info("Loaded configuration from file.");
				}
			} catch (IOException e) {
				LOGGER.error("Failed to read the configuration file.", e);
				createDefaultConfig();
			}
		} else {
			createDefaultConfig();
		}
	}

	private void saveConfig() {
		JsonObject json = new JsonObject();
		json.addProperty("activeForFishing", activeForFishing);
		json.addProperty("activeForBrushing", activeForBrushing);
		json.addProperty("onlyPlayerOwned", onlyPlayerOwned);
		json.addProperty("itemLimit", itemLimit);
		json.addProperty("fishingChance", fishingChance);
		json.addProperty("brushChance", brushChance);
		json.addProperty("luckMultiplier", luckMultiplier);
		json.addProperty("global_broadcast", globalBroadcast);

		CONFIG_FILE.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
			GSON.toJson(json, writer);
		} catch (IOException e) {
			LOGGER.error("Failed to write the configuration file.", e);
		}
	}

	private void createDefaultConfig() {
		activeForFishing = true;
		activeForBrushing = true;
		onlyPlayerOwned = false;
		itemLimit = 10;
		fishingChance = 100;
		brushChance = 10;
		luckMultiplier = 3;
		globalBroadcast = false;

		saveConfig();
		LOGGER.info("Created default configuration file.");
	}

	private static final int LIST_PER_LINE = 4;

	private static int listPool(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		if (despawnedItemManager == null) {
			source.sendFailure(Component.literal("Pool not loaded yet."));
			return 0;
		}
		List<DespawnedItemManager.itemData> pool = despawnedItemManager.snapshot();
		if (pool.isEmpty()) {
			source.sendSuccess(() -> Component.literal("Lost loot pool is empty.").withStyle(ChatFormatting.GRAY), false);
			return Command.SINGLE_SUCCESS;
		}

		source.sendSuccess(() -> Component.literal("Lost loot pool (" + pool.size() + "):")
				.withStyle(ChatFormatting.GOLD), false);

		MutableComponent line = Component.literal("");
		int onLine = 0;
		for (int i = 0; i < pool.size(); i++) {
			DespawnedItemManager.itemData data = pool.get(i);
			if (onLine > 0) {
				line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
			}
			line.append(FindMessages.listEntry(data.item, data.owner, data.time));
			onLine++;
			if (onLine >= LIST_PER_LINE || i == pool.size() - 1) {
				MutableComponent toSend = line;
				source.sendSuccess(() -> toSend, false);
				line = Component.literal("");
				onLine = 0;
			}
		}
		return Command.SINGLE_SUCCESS;
	}
}
