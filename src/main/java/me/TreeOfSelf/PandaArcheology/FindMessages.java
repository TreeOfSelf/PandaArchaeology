package me.TreeOfSelf.PandaArcheology;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class FindMessages {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private FindMessages() {}

    public static Component itemHoverName(ItemStack stack) {
        ItemStackTemplate template = ItemStackTemplate.fromStack(stack);
        return stack.getStyledHoverName().copy().withStyle(style ->
                style.withHoverEvent(new HoverEvent.ShowItem(template)));
    }

    public static Component buildFindMessage(Player finder, DespawnedItemManager.itemData data) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(data.time), ZoneId.systemDefault());
        String formattedDate = dateTime.format(DATE);
        Component itemName = itemHoverName(data.item);

        MutableComponent msg = Component.literal("");
        if (PandaArcheology.globalBroadcast) {
            msg.append(finder.getName().copy().withStyle(ChatFormatting.YELLOW));
            msg.append(Component.literal(" found ").withStyle(ChatFormatting.GRAY));
        } else {
            msg.append(Component.literal("You found ").withStyle(ChatFormatting.GRAY));
        }
        msg.append(itemName);
        if (data.owner != null && !data.owner.isBlank()) {
            msg.append(Component.literal(" dropped by ").withStyle(ChatFormatting.GRAY));
            msg.append(Component.literal(data.owner).withStyle(ChatFormatting.YELLOW));
        }
        msg.append(Component.literal(" on " + formattedDate + ".").withStyle(ChatFormatting.GRAY));
        return msg;
    }

    public static void announce(Player finder, DespawnedItemManager.itemData data) {
        Component message = buildFindMessage(finder, data);
        if (PandaArcheology.globalBroadcast) {
            MinecraftServer server = finder.level().getServer();
            if (server != null) {
                server.getPlayerList().broadcastSystemMessage(message, false);
                return;
            }
        }
        finder.sendSystemMessage(message);
    }

    public static Component listEntry(ItemStack stack, String owner, long timeMs) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timeMs), ZoneId.systemDefault());
        String date = dateTime.format(DATE);
        String who = (owner == null || owner.isBlank()) ? "?" : owner;

        MutableComponent entry = Component.literal("");
        entry.append(itemHoverName(stack));
        entry.append(Component.literal(" (" + who + ", " + date + ")").withStyle(ChatFormatting.DARK_GRAY));
        return entry;
    }
}
