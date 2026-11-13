package me.TreeOfSelf.PandaArcheology;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class ItemFilter {

    public enum Mode {
        NONE, INCLUDE, EXCLUDE;

        static Mode parse(String raw) {
            if (raw == null || raw.isBlank()) return NONE;
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "include" -> INCLUDE;
                case "exclude" -> EXCLUDE;
                default -> NONE;
            };
        }

        String configName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static Mode mode = Mode.NONE;
    public static List<String> rawEntries = List.of();

    private static Set<Item> resolvedItems = Set.of();
    private static List<TagKey<Item>> resolvedTags = List.of();
    private static boolean resolved = false;

    private ItemFilter() {}

    public static void invalidate() {
        resolved = false;
    }

    private static void ensureResolved() {
        if (resolved) return;
        Set<Item> items = new HashSet<>();
        List<TagKey<Item>> tags = new ArrayList<>();
        for (String entry : rawEntries) {
            if (entry == null || entry.isBlank()) continue;
            String s = entry.trim();
            if (s.startsWith("#")) {
                Identifier id = Identifier.tryParse(s.substring(1));
                if (id == null) {
                    PandaArcheology.LOGGER.warn("[ItemFilter] Invalid tag id: {}", s);
                    continue;
                }
                tags.add(TagKey.create(Registries.ITEM, id));
            } else {
                Identifier id = Identifier.tryParse(s);
                if (id == null) {
                    PandaArcheology.LOGGER.warn("[ItemFilter] Invalid item id: {}", s);
                    continue;
                }
                Optional<Item> opt = BuiltInRegistries.ITEM.getOptional(id);
                if (opt.isEmpty() || opt.get() == Items.AIR) {
                    PandaArcheology.LOGGER.warn("[ItemFilter] Unknown item: {}", s);
                    continue;
                }
                items.add(opt.get());
            }
        }
        resolvedItems = Set.copyOf(items);
        resolvedTags = List.copyOf(tags);
        resolved = true;
        PandaArcheology.LOGGER.info("[ItemFilter] mode={} items={} tags={}",
                mode.configName(), resolvedItems.size(), resolvedTags.size());
    }

    public static boolean allows(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (mode == Mode.NONE) return true;
        ensureResolved();

        Item item = stack.getItem();
        boolean matched = resolvedItems.contains(item);
        if (!matched && !resolvedTags.isEmpty()) {
            var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
            for (TagKey<Item> tag : resolvedTags) {
                if (holder.is(tag)) {
                    matched = true;
                    break;
                }
            }
        }
        return (mode == Mode.INCLUDE) == matched;
    }
}
