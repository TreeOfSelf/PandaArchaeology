package me.sebastian420.PandaArcheology.mixin;

import me.sebastian420.PandaArcheology.DespawnedItemManager;
import me.sebastian420.PandaArcheology.PandaArcheology;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Mixin(BrushableBlockEntity.class)
public class BrushBlockMixin {

    @Shadow private ItemStack item;

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/block/entity/BrushableBlockEntity;generateItem(Lnet/minecraft/entity/player/PlayerEntity;)V"), method = "spawnItem")
    private void spawnItem(PlayerEntity player, CallbackInfo ci) {
        if (PandaArcheology.despawnedItemManager.itemLength() > 0
                && player.getWorld().random.nextInt(10) - player.getLuck() <= 0) {

            DespawnedItemManager.itemData itemData = PandaArcheology.despawnedItemManager.getItem(player.getWorld().random);
            String ownerName = itemData.owner;

            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(itemData.time), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            String formattedDate = dateTime.format(formatter);

            if (!ownerName.isBlank() && !ownerName.isEmpty()) {
                player.sendMessage(Text.of("You found something dropped by " + ownerName + " on "+formattedDate+"."));
            } else {
                player.sendMessage(Text.of("You found something dropped on "+formattedDate+"."));
            }

            this.item = itemData.item;
        }
    }
}
