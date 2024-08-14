package me.sebastian420.PandaArcheology.mixin;

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

@Mixin(BrushableBlockEntity.class)
public class BrushBlockMixin {

    @Shadow private ItemStack item;

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/block/entity/BrushableBlockEntity;generateItem(Lnet/minecraft/entity/player/PlayerEntity;)V"), method = "spawnItem")
    private void spawnItem(PlayerEntity player, CallbackInfo ci) {
        if (PandaArcheology.despawnedItemManager.itemLength() > 0
                && player.getWorld().random.nextInt(10) <= 0)  {
            Pair<ItemStack, String> itemData = PandaArcheology.despawnedItemManager.getItem(player.getWorld().random);
            String ownerName = itemData.getRight();
            if (!ownerName.isBlank() && !ownerName.isEmpty())
                player.sendMessage(Text.of("You found something dropped by "+ownerName+"."));
            this.item = itemData.getLeft();
        }
    }
}
