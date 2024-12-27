package me.sebastian420.PandaArcheology.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;



@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements Inventory, Nameable {

    @Shadow
    @Final
    private List<DefaultedList<ItemStack>> combinedInventory;

    @Shadow
    @Final
    public PlayerEntity player;

    @Inject(at=@At(value = "INVOKE"), method = "dropAll()V")
    public void onDropAll(CallbackInfo ci){
        for (List<ItemStack> list : this.combinedInventory) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack itemStack = (ItemStack)list.get(i);
                if (!itemStack.isEmpty()) {
                    this.player.dropItem(itemStack, true, true);
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }
    }
}
