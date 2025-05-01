package me.sebastian420.PandaArcheology.mixin;

import me.sebastian420.PandaArcheology.DespawnedItemManager;
import me.sebastian420.PandaArcheology.PandaArcheology;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/block/entity/BrushableBlockEntity;markDirty()V"), method = "generateItem")
    private void generateItem(ServerWorld world, LivingEntity brusher, ItemStack brush, CallbackInfo ci) {
        if (brusher instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) brusher;

            if (PandaArcheology.activeForBrushing &&
                    PandaArcheology.despawnedItemManager.itemLength() > 0 &&
                    player.getWorld().random.nextInt(PandaArcheology.brushChance) - player.getLuck() <= 0) {

                DespawnedItemManager.itemData itemData = PandaArcheology.despawnedItemManager.getItem(player.getWorld().random);
                String ownerName = itemData.owner;

                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(itemData.time), ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                String formattedDate = dateTime.format(formatter);

                if (!ownerName.isBlank() && !ownerName.isEmpty()) {
                    player.sendMessage(Text.of("You found " + itemData.item.getName().getString() + " dropped by " + ownerName + " on " + formattedDate + "."), false);
                } else {
                    player.sendMessage(Text.of("You found " + itemData.item.getName().getString() + " dropped on " + formattedDate + "."), false);
                }

                this.item = itemData.item;
            }
        }
    }
}
