package dev.vladeezyy.kilnwork.fabric.transfer;

import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import java.util.function.Function;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * Exposes the fluid handler of a Kilnwork item as a Fabric fluid storage of a container item context:
 * changes are made on a copy of the stack, which is then exchanged in the context.
 */
public class FluidHandlerItemStorage implements Storage<FluidVariant> {

    private final ContainerItemContext context;
    private final Function<ItemStack, IFluidHandlerItem> provider;

    public FluidHandlerItemStorage(ContainerItemContext context, Function<ItemStack, IFluidHandlerItem> provider) {
        this.context = context;
        this.provider = provider;
    }

    @Nullable
    private IFluidHandlerItem handler() {
        ItemVariant variant = context.getItemVariant();
        if (variant.isBlank() || context.getAmount() <= 0) {
            return null;
        }
        return provider.apply(variant.toStack(1));
    }

    private long exchange(IFluidHandlerItem handler, TransactionContext transaction) {
        return context.exchange(ItemVariant.of(handler.getContainer()), 1, transaction);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        int mb = FluidAmounts.toMb(maxAmount);
        IFluidHandlerItem handler = handler();
        if (handler == null || resource.isBlank() || mb <= 0) {
            return 0;
        }
        int filled = handler.fill(FluidAmounts.fromVariant(resource, mb), IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0 && exchange(handler, transaction) == 1) {
            return FluidAmounts.toDroplets(filled);
        }
        return 0;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        int mb = FluidAmounts.toMb(maxAmount);
        IFluidHandlerItem handler = handler();
        if (handler == null || resource.isBlank() || mb <= 0) {
            return 0;
        }
        FluidStack drained = handler.drain(FluidAmounts.fromVariant(resource, mb), IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty() && exchange(handler, transaction) == 1) {
            return FluidAmounts.toDroplets(drained.getAmount());
        }
        return 0;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        IFluidHandlerItem handler = handler();
        if (handler == null) {
            return java.util.Collections.emptyIterator();
        }
        List<StorageView<FluidVariant>> views = Lists.newArrayListWithCapacity(handler.getTanks());
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack fluid = handler.getFluidInTank(i);
            long capacity = FluidAmounts.toDroplets(handler.getTankCapacity(i));
            views.add(new StorageView<>() {
                @Override
                public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                    return fluid.isEmpty() || !resource.equals(FluidAmounts.toVariant(fluid)) ? 0
                            : FluidHandlerItemStorage.this.extract(resource, maxAmount, transaction);
                }

                @Override
                public boolean isResourceBlank() {
                    return fluid.isEmpty();
                }

                @Override
                public FluidVariant getResource() {
                    return FluidAmounts.toVariant(fluid);
                }

                @Override
                public long getAmount() {
                    return FluidAmounts.toDroplets(fluid.getAmount());
                }

                @Override
                public long getCapacity() {
                    return capacity;
                }
            });
        }
        return views.iterator();
    }
}
