package me.TreeOfSelf.PandaArcheology.mixin;
import me.TreeOfSelf.PandaArcheology.DespawnedItemManager;
import me.TreeOfSelf.PandaArcheology.PandaArcheology;
import me.TreeOfSelf.PandaArcheology.FindMessages;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BrushableBlockEntity.class)
public abstract class BrushBlockMixin {

	@Shadow
	private ItemStack item;

	@Inject(method = "unpackLootTable", at = @At("TAIL"))
	private void pandaAfterArchaeologyLoot(ServerLevel level, LivingEntity user, ItemInstance brush, CallbackInfo ci) {
		if (!(user instanceof ServerPlayer player)) {
			return;
		}
		if (this.item.isEmpty()) {
			return;
		}
		if (PandaArcheology.activeForBrushing
				&& PandaArcheology.despawnedItemManager.itemLength() > 0
				&& player.level().getRandom().nextInt(PandaArcheology.brushChance) - (int) (player.getLuck() * PandaArcheology.luckMultiplier) <= 0) {
			DespawnedItemManager.itemData itemData = PandaArcheology.despawnedItemManager.getItem(player.level().getRandom());
			FindMessages.announce(player, itemData);
			this.item = itemData.item;
			((BlockEntity) (Object) this).setChanged();
		}
	}
}
