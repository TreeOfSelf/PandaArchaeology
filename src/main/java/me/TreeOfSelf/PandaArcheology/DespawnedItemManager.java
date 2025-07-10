package me.TreeOfSelf.PandaArcheology;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
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
                nbtList.add(ItemStack.CODEC.encodeStart(this.registryManager.getOps(NbtOps.INSTANCE), despawnedItems.get(x)).getOrThrow());
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
                NbtCompound thisItem = nbtList.get().getCompound(i).get();
                DataResult<Pair<ItemStack, NbtElement>> item = ItemStack.CODEC.decode(registryManager.getOps(NbtOps.INSTANCE), thisItem);
                ItemStack loadedStack = item.getOrThrow().getFirst();
                
                if (loadedStack.contains(DataComponentTypes.ITEM_NAME)) {
                    Text itemName = loadedStack.get(DataComponentTypes.ITEM_NAME);
                    if (itemName != null && itemName.getString().startsWith("{")) {
                        try {
                            JsonElement jsonElement = JsonParser.parseString(itemName.getString());
                            DataResult<Pair<Text, JsonElement>> result = TextCodecs.CODEC.decode(registryManager.getOps(JsonOps.INSTANCE), jsonElement);
                            loadedStack.set(DataComponentTypes.ITEM_NAME, result.getOrThrow().getFirst());
                        } catch (Exception ignored) {
                        }
                    }
                }
                
                if (loadedStack.contains(DataComponentTypes.CUSTOM_NAME)) {
                    Text customName = loadedStack.get(DataComponentTypes.CUSTOM_NAME);
                    if (customName != null && customName.getString().startsWith("{")) {
                        try {
                            JsonElement jsonElement = JsonParser.parseString(customName.getString());
                            DataResult<Pair<Text, JsonElement>> result = TextCodecs.CODEC.decode(registryManager.getOps(JsonOps.INSTANCE), jsonElement);
                            loadedStack.set(DataComponentTypes.CUSTOM_NAME, result.getOrThrow().getFirst());
                        } catch (Exception ignored) {
                        }
                    }
                }
                
                if (loadedStack.contains(DataComponentTypes.LORE)) {
                    LoreComponent lore = loadedStack.get(DataComponentTypes.LORE);
                    if (lore != null && !lore.lines().isEmpty()) {
                        List<Text> newLoreLines = new ArrayList<>();
                        for (Text line : lore.lines()) {
                            Text newLine = line;
                            if (line.getString().startsWith("{")) {
                                try {
                                    JsonElement jsonElement = JsonParser.parseString(line.getString());
                                    DataResult<Pair<Text, JsonElement>> result = TextCodecs.CODEC.decode(registryManager.getOps(JsonOps.INSTANCE), jsonElement);
                                    newLine = result.getOrThrow().getFirst();
                                } catch (Exception ignored) {
                                }
                            }
                            newLoreLines.add(newLine);
                        }
                        loadedStack.set(DataComponentTypes.LORE, new LoreComponent(newLoreLines));
                    }
                }
                
                despawnedItems.add(loadedStack);
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
