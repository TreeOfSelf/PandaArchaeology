package me.TreeOfSelf.PandaArcheology;

import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import me.TreeOfSelf.PandaArcheology.DespawnedItemManager.itemData;

import java.util.ArrayList;
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
	public static int globalItemLimit;
	public static int fishingChance;
	public static int brushChance;
	public static int luckMultiplier;
	public static boolean globalBroadcast;
	private static List<String> defaultFilterEntries() {
		return List.of(
				"minecraft:dirt",
				"minecraft:cobblestone",
				"minecraft:kelp",
				"minecraft:egg",
				"minecraft:sand",
				"minecraft:gravel"
		);
	}

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		ServerLifecycleEvents.AFTER_SAVE.register(this::onServerSave);
		loadOrCreateConfig();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
				Commands.literal("lostloot")

					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

						.then(Commands.literal("list")
							.executes(PandaArcheology::listPool)
						)

						.then(Commands.literal("remove")
							.then(Commands.argument("id", IntegerArgumentType.integer())
								.executes(context -> {
									int id = context.getArgument("id", Integer.class);
									return PandaArcheology.remItem(context, id-1);
								})
							)
						)

						.then(Commands.literal("clear")
							.executes(PandaArcheology::clearPool)
						)

						.then(Commands.literal("recover")
							.then(Commands.argument("id", IntegerArgumentType.integer())
								.executes(context -> {
									if (context.getSource().getPlayer() == null) {
										// should only happen with rcon and server command, maybe?
										context.getSource().sendFailure(Component.literal("Please, execute recover command as player."));
										return 0;
									}
									int id = context.getArgument("id", Integer.class);
									return PandaArcheology.recoverItem(context, id-1);
								})
							)
						)
			);
		});
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
		ItemFilter.invalidate();
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

				if (json.has("globalItemLimit")) {
					globalItemLimit = json.get("globalItemLimit").getAsInt();
				} else {
					globalItemLimit = 256;
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

				String filterMode = "exclude";
				if (json.has("item_filter_mode")) {
					filterMode = json.get("item_filter_mode").getAsString();
				} else {
					needsUpdate = true;
				}

				List<String> filterEntries = new ArrayList<>();
				if (json.has("item_filter") && json.get("item_filter").isJsonArray()) {
					for (JsonElement el : json.getAsJsonArray("item_filter")) {
						if (el.isJsonPrimitive()) filterEntries.add(el.getAsString());
					}
				} else {
					filterEntries.addAll(defaultFilterEntries());
					needsUpdate = true;
				}
				ItemFilter.mode = ItemFilter.Mode.parse(filterMode);
				ItemFilter.rawEntries = List.copyOf(filterEntries);
				ItemFilter.invalidate();

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
		json.addProperty("globalItemLimit", globalItemLimit);
		json.addProperty("fishingChance", fishingChance);
		json.addProperty("brushChance", brushChance);
		json.addProperty("luckMultiplier", luckMultiplier);
		json.addProperty("global_broadcast", globalBroadcast);
		json.addProperty("item_filter_mode", ItemFilter.mode.configName());
		JsonArray filterArr = new JsonArray();
		for (String entry : ItemFilter.rawEntries) filterArr.add(entry);
		json.add("item_filter", filterArr);

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
		onlyPlayerOwned = true;
		itemLimit = 10;
		globalItemLimit = 256;
		fishingChance = 100;
		brushChance = 10;
		luckMultiplier = 3;
		globalBroadcast = false;

		saveConfig();
		LOGGER.info("Created default configuration file.");
	}

	private static int recoverItem(CommandContext<CommandSourceStack> ctx, int id) {
		CommandSourceStack source = ctx.getSource();

		if (despawnedItemManager == null) {
			source.sendFailure(Component.literal("Pool not loaded yet."));
			return 0;
		}

		if (id < 0 || id >= despawnedItemManager.itemLength()) {
			source.sendFailure(Component.literal("Invalide item id: %d. Use /lostloot list to see valid identifiers.".formatted(id+1)));
			return 0;
		}

		ServerPlayer player = source.getPlayer();
		assert player != null;  // checked in dispatcher, assertion to sooth IntelliJ

		itemData item = despawnedItemManager.getAt(id);
		if (item == null) {
			source.sendFailure(Component.literal("Invalide item at id %d. Use /lostloot list to see valid identifiers.".formatted(id+1)));
			return 0;
		}

		boolean bGiven;
		int slotId = player.getInventory().getFreeSlot();

		if (slotId == -1) {
			// No free slot ;
			// could drop with player.drop(item.item.copy(), false, Prediction.PREDICTED);
			bGiven = false;
		} else {
			// Free slot, insert.
			// Can use add(item.item), but we already got the slot, so why not?
			bGiven = player.getInventory().add(slotId, item.item.copy());
		}

		if (bGiven) {
			if (!despawnedItemManager.removeAt(id)) {
				// the item has already been given, but the admin has to know that the deletion has failed
				source.sendSuccess(() -> Component.literal("Failed to delete the given item (id %02d).".formatted(id+1)).withStyle(ChatFormatting.GRAY), false);
			};

			MutableComponent msg = Component.literal("").withStyle(ChatFormatting.GREEN);
			msg.append(Component.literal("Gave item ["));
			msg.append(Component.literal("%02d".formatted(id+1)).withStyle(ChatFormatting.AQUA));
			msg.append(Component.literal("] "));
			msg.append(item.itemHoverName);
			msg.append(Component.literal(" (lost by "));
			msg.append(Component.literal(item.owner.isBlank() ? "unknown" : item.owner).withStyle(ChatFormatting.YELLOW));
			msg.append(Component.literal(" on "));
			msg.append(Component.literal(item.dateFormated).withStyle(ChatFormatting.LIGHT_PURPLE));
			msg.append(Component.literal(") to "));
			msg.append(Component.literal(player.getPlainTextName()).withStyle(ChatFormatting.GOLD));
			msg.append(Component.literal(" from lost items pool."));

			source.sendSuccess(() -> msg, true);
			return Command.SINGLE_SUCCESS;
		} else {
			MutableComponent msg = Component.literal("");  // .withStyle(ChatFormatting.RED);
			msg.append(Component.literal("Failed to give item ["));
			msg.append(Component.literal("%02d".formatted(id+1)).withStyle(ChatFormatting.AQUA));
			msg.append(Component.literal("] "));
			msg.append(item.itemHoverName);
			msg.append(Component.literal(" (lost by "));
			msg.append(Component.literal(item.owner.isBlank() ? "unknown" : item.owner).withStyle(ChatFormatting.YELLOW));
			msg.append(Component.literal(" on "));
			msg.append(Component.literal(item.dateFormated).withStyle(ChatFormatting.LIGHT_PURPLE));
			msg.append(Component.literal(") to "));
			msg.append(Component.literal(player.getPlainTextName()).withStyle(ChatFormatting.GOLD));
			msg.append(Component.literal(". Free a slot up in the inventory."));

			source.sendFailure(msg);
			return 0;
		}
	}

	private static int remItem(CommandContext<CommandSourceStack> ctx, int id) {
		CommandSourceStack source = ctx.getSource();

		if (despawnedItemManager == null) {
			source.sendFailure(Component.literal("Pool not loaded yet."));
			return 0;
		}

		if (id < 0 || id >= despawnedItemManager.itemLength()) {
			source.sendFailure(Component.literal("Invalide item id: %d. Use /lostloot list to see valid identifiers.".formatted(id+1)));
			return 0;
		}

		if (despawnedItemManager.removeAt(id)) {
			source.sendSuccess(() -> Component.literal("Removed item [%d] from the lost loot pool.".formatted(id+1)).withStyle(ChatFormatting.GREEN), true);
			return Command.SINGLE_SUCCESS;
		} else {
			source.sendFailure(Component.literal("Failed to remove item with id %d".formatted(id+1)).withStyle(ChatFormatting.RED));
			return 0;
		}

	}

	private static int clearPool(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();

		if (despawnedItemManager == null) {
			source.sendFailure(Component.literal("Pool not loaded yet."));
			return 0;
		}

		int removedCount = despawnedItemManager.clear();
		if (removedCount > 0) {
			source.sendSuccess(() -> Component.literal("Correctly emptied %d items from the pool.".formatted(removedCount)).withStyle(ChatFormatting.GREEN), true);
        } else {
			source.sendSuccess(() -> Component.literal("The pool was already empty.").withStyle(ChatFormatting.GRAY), false);
        }
        return Command.SINGLE_SUCCESS;
    }

	private static final int LIST_PER_LINE = 1;

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

		source.sendSuccess(() -> Component.literal("Lost loot pool (%d):".formatted(pool.size()))
				.withStyle(ChatFormatting.GOLD), false);

		MutableComponent line = Component.literal("");
		int onLine = 0;
		for (int i = 0; i < pool.size(); i++) {
			DespawnedItemManager.itemData data = pool.get(i);
			if (onLine > 0) {
				line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
			}
			line.append("[%02d] ".formatted(i+1)).withStyle(ChatFormatting.RED);
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
