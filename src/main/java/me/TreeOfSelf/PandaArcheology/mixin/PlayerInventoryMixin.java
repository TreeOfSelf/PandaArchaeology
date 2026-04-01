package me.TreeOfSelf.PandaArcheology.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

	@Shadow
	@Final
	public Player player;

	@Redirect(
			method = "dropAll",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"
			)
	)
	private ItemEntity redirectDropItem(Player playerEntity, ItemStack stack, boolean randomly, boolean thrownFromHand) {
		return playerEntity.drop(stack, true, true);
	}
}
