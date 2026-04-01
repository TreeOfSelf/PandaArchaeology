package me.TreeOfSelf.PandaArcheology;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

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
    private final HolderLookup.Provider registries;

    public DespawnedItemManager(Path worldSavePath, HolderLookup.Provider registries) {
        this.dataFile = worldSavePath.resolve("despawned_items.dat");
        this.registries = registries;
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

    public itemData getItem(RandomSource random) {
        int index = random.nextInt(despawnedItems.size());
        return new itemData(despawnedItems.remove(index), despawnedItemsOwners.remove(index), despawnedItemsTimes.remove(index));
    }

    public int itemLength() {
        return despawnedItems.size();
    }

    public void save() {
        CompoundTag compound = new CompoundTag();
        ListTag nbtList = new ListTag();
        ListTag nbtListOwners = new ListTag();
        ListTag nbtListTimes = new ListTag();

        for (int x = 0; x < despawnedItems.size(); x++) {
            try {
                Tag encoded = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), despawnedItems.get(x)).getOrThrow();
                nbtList.add(encoded);
                nbtListOwners.add(StringTag.valueOf(despawnedItemsOwners.get(x)));
                long[] tempArray = new long[1];
                tempArray[0] = despawnedItemsTimes.get(x);
                nbtListTimes.add(new LongArrayTag(tempArray));
            } catch (Exception ignored) {
            }
        }

        compound.put("DespawnedItems", nbtList);
        compound.put("DespawnedOwners", nbtListOwners);
        compound.put("DespawnedTimes", nbtListTimes);

        try {
            NbtIo.writeCompressed(compound, dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!dataFile.toFile().exists()) {
            return;
        }

        try {
            CompoundTag compound = NbtIo.readCompressed(dataFile, NbtAccounter.unlimitedHeap());
            Optional<ListTag> nbtList = compound.getList("DespawnedItems");
            Optional<ListTag> nbtListOwners = compound.getList("DespawnedOwners");
            Optional<ListTag> nbtListTimes = compound.getList("DespawnedTimes");

            if (nbtList.isEmpty() || nbtListOwners.isEmpty() || nbtListTimes.isEmpty()) {
                return;
            }

            ListTag items = nbtList.get();
            ListTag owners = nbtListOwners.get();
            ListTag times = nbtListTimes.get();

            for (int i = 0; i < items.size(); i++) {
                try {
                    Optional<CompoundTag> optCompound = items.getCompound(i);
                    if (optCompound.isEmpty()) {
                        continue;
                    }
                    CompoundTag thisItem = optCompound.get();
                    DataResult<Pair<ItemStack, Tag>> item = ItemStack.CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), thisItem);
                    ItemStack loadedStack = item.getOrThrow().getFirst();

                    if (loadedStack.has(DataComponents.ITEM_NAME)) {
                        Component itemName = loadedStack.get(DataComponents.ITEM_NAME);
                        if (itemName != null && itemName.getString().startsWith("{")) {
                            try {
                                JsonElement jsonElement = JsonParser.parseString(itemName.getString());
                                DataResult<Pair<Component, JsonElement>> result = ComponentSerialization.CODEC.decode(registries.createSerializationContext(JsonOps.INSTANCE), jsonElement);
                                loadedStack.set(DataComponents.ITEM_NAME, result.getOrThrow().getFirst());
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    if (loadedStack.has(DataComponents.CUSTOM_NAME)) {
                        Component customName = loadedStack.get(DataComponents.CUSTOM_NAME);
                        if (customName != null && customName.getString().startsWith("{")) {
                            try {
                                JsonElement jsonElement = JsonParser.parseString(customName.getString());
                                DataResult<Pair<Component, JsonElement>> result = ComponentSerialization.CODEC.decode(registries.createSerializationContext(JsonOps.INSTANCE), jsonElement);
                                loadedStack.set(DataComponents.CUSTOM_NAME, result.getOrThrow().getFirst());
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    if (loadedStack.has(DataComponents.LORE)) {
                        ItemLore lore = loadedStack.get(DataComponents.LORE);
                        if (lore != null && !lore.lines().isEmpty()) {
                            List<Component> newLoreLines = new ArrayList<>();
                            for (Component line : lore.lines()) {
                                Component newLine = line;
                                if (line.getString().startsWith("{")) {
                                    try {
                                        JsonElement jsonElement = JsonParser.parseString(line.getString());
                                        DataResult<Pair<Component, JsonElement>> result = ComponentSerialization.CODEC.decode(registries.createSerializationContext(JsonOps.INSTANCE), jsonElement);
                                        newLine = result.getOrThrow().getFirst();
                                    } catch (Exception ignored) {
                                    }
                                }
                                newLoreLines.add(newLine);
                            }
                            loadedStack.set(DataComponents.LORE, new ItemLore(newLoreLines));
                        }
                    }

                    despawnedItems.add(loadedStack);
                    despawnedItemsOwners.add(owners.getStringOr(i, ""));
                    Optional<long[]> timeOpt = times.getLongArray(i);
                    if (timeOpt.isPresent() && timeOpt.get().length > 0) {
                        despawnedItemsTimes.add(timeOpt.get()[0]);
                    } else {
                        despawnedItemsTimes.add(0L);
                    }
                } catch (Exception e) {
                    PandaArcheology.LOGGER.info("Skipping despawned item at index {} due to error: {}", i, e.getMessage());
                }
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
