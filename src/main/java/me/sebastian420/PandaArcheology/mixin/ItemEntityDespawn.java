package me.sebastian420.PandaArcheology.mixin;

import me.sebastian420.PandaArcheology.PandaArcheology;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityDespawn {
	@Shadow public abstract ItemStack getStack();
	@Shadow @Nullable public abstract Entity getOwner();

	@Inject(at = @At(value = "INVOKE",target = "Lnet/minecraft/entity/ItemEntity;discard()V", ordinal = 1), method = "tick")
	private void despawned(CallbackInfo info) {

		String nameString = "";
		if (this.getOwner() != null) {
			nameString = this.getOwner().getName().getString();
			PandaArcheology.despawnedItemManager.addItem(this.getStack(), nameString);
		}

	}


}