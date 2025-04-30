package me.sebastian420.PandaArcheology;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.random.Random;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DespawnedItemManager {
    private final List<ItemStack> despawnedItems = new ArrayList<>();
    private final List<String> despawnedItemsOwners = new ArrayList<>();
    private final List<Long> despawnedItemsTimes = new ArrayList<>();


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

        if (occurences < PandaArcheology.itemLimit || PandaArcheology.itemLimit == 0) {
            despawnedItems.add(item);
            despawnedItemsOwners.add(string);
            despawnedItemsTimes.add(System.currentTimeMillis());
        }
    }

    public itemData getItem(Random random) {
        int index = random.nextInt(despawnedItems.size());
        return new itemData(despawnedItems.remove(index), despawnedItemsOwners.remove(index), despawnedItemsTimes.remove(index));
    }

    public int itemLength() {
        return despawnedItems.size();
    }


    public void save() {
        NbtCompound compound = new NbtCompound();
        NbtList nbtList = new NbtList();
        NbtList nbtListOwners = new NbtList();
        NbtList nbtListTimes = new NbtList();

        for (int x = 0; x<despawnedItems.size(); x++) {
            try {
                nbtList.add(despawnedItems.get(x).toNbt(registryManager));
                nbtListOwners.add(NbtString.of(despawnedItemsOwners.get(x)));

                long[] tempArray = new long[1];
                tempArray[0] = despawnedItemsTimes.get(x);
                nbtListTimes.add(new NbtLongArray(tempArray));
            } catch(Exception ignored){}
        }

        compound.put("DespawnedItems", nbtList);
        compound.put("DespawnedOwners", nbtListOwners);
        compound.put("DespawnedTimes", nbtListTimes);

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
            Optional<NbtList> nbtList = compound.getList("DespawnedItems");
            Optional<NbtList> nbtListOwners = compound.getList("DespawnedOwners");
            Optional<NbtList> nbtListTimes = compound.getList("DespawnedTimes");


            for (int i = 0; i < nbtList.get().size(); i++) {
                Optional<NbtCompound> itemCompound = nbtList.get().getCompound(i);
                Optional<ItemStack> item = ItemStack.fromNbt(registryManager,itemCompound.get());
                despawnedItems.add(item.get());
                despawnedItemsOwners.add(nbtListOwners.get().get(i).asString().get());
                despawnedItemsTimes.add(nbtListTimes.get().getLongArray(i).get()[0]);

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class itemData {
        public ItemStack item;
        public String owner;
        public long time;

        itemData(ItemStack item, String owner, long time) {
            this.item = item;
            this.owner = owner;
            this.time = time;
        }
    }
}
