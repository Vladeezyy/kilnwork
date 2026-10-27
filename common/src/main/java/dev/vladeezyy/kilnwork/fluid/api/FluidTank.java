package dev.vladeezyy.kilnwork.fluid.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidTank;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * A tank with a single fluid and a capacity in millibuckets.
 */
public class FluidTank implements IFluidHandler, IFluidTank {

    protected Predicate<FluidStack> validator;
    protected FluidStack fluid = FluidStack.EMPTY;
    protected int capacity;

    public FluidTank(int capacity) {
        this(capacity, e -> true);
    }

    public FluidTank(int capacity, Predicate<FluidStack> validator) {
        this.capacity = capacity;
        this.validator = validator;
    }

    public FluidTank setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public FluidTank setValidator(Predicate<FluidStack> validator) {
        if (validator != null) {
            this.validator = validator;
        }
        return this;
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
        return validator.test(stack);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public FluidStack getFluid() {
        return fluid;
    }

    @Override
    public int getFluidAmount() {
        return fluid.getAmount();
    }

    public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        fluid = FluidStack.parseOptional(lookupProvider, nbt.getCompound("Fluid"));
        return this;
    }

    public CompoundTag writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        if (!fluid.isEmpty()) {
            nbt.put("Fluid", fluid.save(lookupProvider));
        }
        return nbt;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(resource)) {
            return 0;
        }
        if (fluid.isEmpty()) {
            int filled = Math.min(capacity, resource.getAmount());
            if (action.execute() && filled > 0) {
                fluid = resource.copyWithAmount(filled);
                onContentsChanged();
            }
            return filled;
        }
        if (!FluidStack.isSameFluidSameComponents(fluid, resource)) {
            return 0;
        }
        int filled = Math.max(0, Math.min(capacity - fluid.getAmount(), resource.getAmount()));
        if (action.execute() && filled > 0) {
            fluid.grow(filled);
            onContentsChanged();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, fluid)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int drained = Math.min(maxDrain, fluid.getAmount());
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack stack = fluid.copyWithAmount(drained);
        if (action.execute()) {
            fluid.shrink(drained);
            if (fluid.isEmpty()) {
                fluid = FluidStack.EMPTY;
            }
            onContentsChanged();
        }
        return stack;
    }

    /** Puts back an earlier content exactly (an aborted Fabric transaction), notifying the tank of the change */
    public void restoreFluid(FluidStack stack) {
        setFluid(stack.copy());
        onContentsChanged();
    }

    protected void onContentsChanged() {
    }

    public void setFluid(@Nullable FluidStack stack) {
        this.fluid = stack == null ? FluidStack.EMPTY : stack;
    }

    public boolean isEmpty() {
        return fluid.isEmpty();
    }

    public int getSpace() {
        return Math.max(0, capacity - fluid.getAmount());
    }
}
