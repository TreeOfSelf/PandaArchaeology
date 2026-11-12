package me.TreeOfSelf.PandaArcheology.mixin;
import me.TreeOfSelf.PandaArcheology.FindMessages;
import me.TreeOfSelf.PandaArcheology.DespawnedItemManager;
import me.TreeOfSelf.PandaArcheology.PandaArcheology;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(FishingHook.class)
public abstract class FishCatchMixin {
	@Shadow
	public abstract Player getPlayerOwner();

	@Shadow
	private int luck;

	@Shadow
	private boolean openWater;

	@Redirect(
			method = "retrieve",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
			)
	)
	private ObjectArrayList<ItemStack> pandaRedirectFishLoot(LootTable lootTable, LootParams params) {
		ObjectArrayList<ItemStack> items = lootTable.getRandomItems(params);
		Player owner = this.getPlayerOwner();
		if (!PandaArcheology.activeForFishing || owner == null || PandaArcheology.despawnedItemManager.itemLength() <= 0) {
			return items;
		}
		if (!this.openWater) {
			return items;
		}
		if (owner.level().getRandom().nextInt(PandaArcheology.fishingChance) - (this.luck + owner.getLuck()) * PandaArcheology.luckMultiplier > 0) {
			return items;
		}
		if (items.isEmpty()) {
			return items;
		}
		DespawnedItemManager.itemData itemData = PandaArcheology.despawnedItemManager.getItem(owner.level().getRandom());
		FindMessages.announce(owner, itemData);
		items.set(0, itemData.item);
		return items;
	}
}
