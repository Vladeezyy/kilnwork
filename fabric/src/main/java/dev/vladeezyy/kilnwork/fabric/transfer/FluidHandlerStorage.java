package dev.vladeezyy.kilnwork.fabric.transfer;

import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.FluidTank;

import javax.annotation.Nullable;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;

import java.util.Iterator;
import java.util.List;

/**
 * Exposes an Kilnwork fluid handler as a Fabric fluid storage.
 * Changes are applied at once and undone if the transaction is aborted.
 */
public class FluidHandlerStorage implements Storage<FluidVariant> {

    private final IFluidHandler handler;

    public FluidHandlerStorage(IFluidHandler handler) {
        this.handler = handler;
    }

    public IFluidHandler getHandler() {
        return handler;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        int mb = FluidAmounts.toMb(maxAmount);
        if (resource.isBlank() || mb <= 0) {
            return 0;
        }
        FluidStack stack = FluidAmounts.fromVariant(resource, mb);
        if (handler.fill(stack, IFluidHandler.FluidAction.SIMULATE) <= 0) {
            return 0;
        }
        Runnable restore = restorer(stack.copyWithAmount(1), true);
        int filled = handler.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            UndoCallback.register(transaction, restore != null ? restore
                    : () -> handler.drain(stack.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE));
        }
        return FluidAmounts.toDroplets(filled);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        int mb = FluidAmounts.toMb(maxAmount);
        if (resource.isBlank() || mb <= 0) {
            return 0;
        }
        FluidStack request = FluidAmounts.fromVariant(resource, mb);
        if (handler.drain(request, IFluidHandler.FluidAction.SIMULATE).isEmpty()) {
            return 0;
        }
        Runnable restore = restorer(request, false);
        FluidStack drained = handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            FluidStack undo = drained.copy();
            UndoCallback.register(transaction, restore != null ? restore
                    : () -> handler.fill(undo, IFluidHandler.FluidAction.EXECUTE));
        }
        return FluidAmounts.toDroplets(drained.getAmount());
    }

    /**
     * For tanks, the exact previous state which a filling or draining in
     * the opposite direction would not give back.
     */
    @Nullable
    private Runnable restorer(FluidStack change, boolean fill) {
        if (handler instanceof FluidTank tank) {
            FluidStack before = tank.getFluid().copy();
            return () -> {
                tank.restoreFluid(before);
            };
        }
        return null;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        List<StorageView<FluidVariant>> views = Lists.newArrayListWithCapacity(handler.getTanks());
        for (int i = 0; i < handler.getTanks(); i++) {
            views.add(new TankView(i));
        }
        return views.iterator();
    }

    private class TankView implements StorageView<FluidVariant> {
        private final int tank;

        private TankView(int tank) {
            this.tank = tank;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            FluidStack contained = handler.getFluidInTank(tank);
            if (contained.isEmpty() || !resource.equals(FluidAmounts.toVariant(contained))) {
                return 0;
            }
            return FluidHandlerStorage.this.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return handler.getFluidInTank(tank).isEmpty();
        }

        @Override
        public FluidVariant getResource() {
            return FluidAmounts.toVariant(handler.getFluidInTank(tank));
        }

        @Override
        public long getAmount() {
            return FluidAmounts.toDroplets(handler.getFluidInTank(tank).getAmount());
        }

        @Override
        public long getCapacity() {
            return FluidAmounts.toDroplets(handler.getTankCapacity(tank));
        }
    }
}
