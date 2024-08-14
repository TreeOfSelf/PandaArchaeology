package me.sebastian420.PandaArcheology;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PandaArcheology implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("pandaarcheology");
	public static DespawnedItemManager despawnedItemManager;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		ServerLifecycleEvents.AFTER_SAVE.register(this::onServerSave);
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
}