package me.TreeOfSelf.PandaArcheology.mixin;

import me.TreeOfSelf.PandaArcheology.PandaArcheology;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityDespawn {
	@Shadow
	public abstract ItemStack getItem();

	@Shadow
	@Nullable
	public abstract Entity getOwner();

	@Shadow
	private int pickupDelay;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V", ordinal = 1), method = "tick")
	private void despawned(CallbackInfo info) {
		if (this.pickupDelay == 32767) {
			return;
		}
		if (this.getOwner() != null || !PandaArcheology.onlyPlayerOwned) {
			String nameString = "";
			if (this.getOwner() != null) nameString = this.getOwner().getName().getString();
			PandaArcheology.despawnedItemManager.addItem(this.getItem(), nameString);
		}
	}
}
