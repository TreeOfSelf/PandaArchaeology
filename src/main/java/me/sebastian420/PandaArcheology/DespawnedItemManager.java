package me.sebastian420.PandaArcheology;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Pair;
import net.minecraft.util.math.random.Random;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DespawnedItemManager {
    private final List<ItemStack> despawnedItems = new ArrayList<>();
    private final List<String> despawnedItemsOwners = new ArrayList<>();

    private final Path dataFile;
    private final DynamicRegistryManager registryManager;

    public DespawnedItemManager(Path worldSavePath, DynamicRegistryManager registryManager) {
        this.dataFile = worldSavePath.resolve("despawned_items.dat");
        this.registryManager = registryManager;
        load();
    }

    public void addItem(ItemStack item, String string) {
        int occurences = 0;
        for (ItemStack despawnedItem : despawnedItems) {
            if (despawnedItem.getItem() == item.getItem()) occurences++;
        }

        if (occurences < 10) {
            despawnedItems.add(item);
            despawnedItemsOwners.add(string);
        }
    }

    public Pair<ItemStack, String> getItem(Random random) {
        int index = random.nextInt(despawnedItems.size());
        return new Pair<>(despawnedItems.remove(index), despawnedItemsOwners.remove(index));
    }

    public int itemLength() {
        return despawnedItems.size();
    }


    public void save() {
        NbtCompound compound = new NbtCompound();
        NbtList nbtList = new NbtList();
        NbtList nbtListOwners = new NbtList();
        for (int x = 0; x<despawnedItems.size(); x++) {
            nbtList.add(despawnedItems.get(x).encode(registryManager));
            nbtListOwners.add(NbtString.of(despawnedItemsOwners.get(x)));
        }

        compound.put("DespawnedItems", nbtList);
        compound.put("DespawnedOwners", nbtListOwners);

        try {
            NbtIo.writeCompressed(compound, dataFile.toFile().toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!dataFile.toFile().exists()) {
            return;
        }

        try {
            NbtCompound compound = NbtIo.readCompressed(dataFile.toFile().toPath(), NbtSizeTracker.ofUnlimitedBytes());
            NbtList nbtList = compound.getList("DespawnedItems", NbtElement.COMPOUND_TYPE);
            NbtList nbtListOwners = compound.getList("DespawnedOwners", NbtElement.STRING_TYPE);

            for (int i = 0; i < nbtList.size(); i++) {
                NbtCompound itemCompound = nbtList.getCompound(i);
                Optional<ItemStack> item = ItemStack.fromNbt(registryManager,itemCompound);
                despawnedItems.add(item.get());
                despawnedItemsOwners.add(nbtListOwners.get(i).asString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
