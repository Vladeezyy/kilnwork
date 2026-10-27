package dev.vladeezyy.kilnwork.fabric.transfer;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.world.item.ItemStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;

import javax.annotation.Nullable;

/**
 * The Fabric fluid storage of an item stack (of another mod or vanilla) seen as an item fluid handler.
 * The stack lives in a private slot, so {@link #getContainer()} gives the (possibly exchanged) result.
 */
public class StorageFluidHandlerItem extends StorageFluidHandler implements IFluidHandlerItem {

    private final Slot slot;

    private StorageFluidHandlerItem(Storage<FluidVariant> storage, Slot slot) {
        super(storage);
        this.slot = slot;
    }

    @Nullable
    public static StorageFluidHandlerItem of(ItemStack stack) {
        Slot slot = new Slot(stack);
        Storage<FluidVariant> storage = FluidStorage.ITEM.find(stack, ContainerItemContext.ofSingleSlot(slot));
        return storage == null ? null : new StorageFluidHandlerItem(storage, slot);
    }

    @Override
    public ItemStack getContainer() {
        return slot.getResource().toStack((int) slot.getAmount());
    }

    private static class Slot extends SingleVariantStorage<ItemVariant> {
        Slot(ItemStack stack) {
            this.variant = ItemVariant.of(stack);
            this.amount = stack.getCount();
        }

        @Override
        protected ItemVariant getBlankVariant() {
            return ItemVariant.blank();
        }

        @Override
        protected long getCapacity(ItemVariant variant) {
            return variant.getItem().getDefaultMaxStackSize();
        }
    }
}
