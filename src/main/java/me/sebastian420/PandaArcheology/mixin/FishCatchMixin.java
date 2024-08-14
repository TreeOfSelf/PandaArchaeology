package me.sebastian420.PandaArcheology.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.sebastian420.PandaArcheology.PandaArcheology;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FishingBobberEntity.class)
public abstract class FishCatchMixin {
    @Shadow @Nullable public abstract PlayerEntity getPlayerOwner();

    @Shadow @Nullable public abstract Entity getHookedEntity();

    @Shadow @Final private int luckBonus;

    @ModifyVariable(method = "use", at = @At(value = "STORE"), ordinal = 1)
    private ItemStack injected(ItemStack value, @Local LootContextParameterSet lootContextParameterSet) {
        if (PandaArcheology.despawnedItemManager.itemLength() <= 0
                || this.getPlayerOwner().getWorld().random.nextInt(50) - this.luckBonus * 10  > 0)  {
            return value;
        } else {
            Pair<ItemStack, String> itemData = PandaArcheology.despawnedItemManager.getItem(this.getPlayerOwner().getWorld().random);
            String ownerName = itemData.getRight();
            if (!ownerName.isBlank() && !ownerName.isEmpty())
                this.getPlayerOwner().sendMessage(Text.of("You found something dropped by "+ownerName+"."));
            return itemData.getLeft();
        }
    }
}