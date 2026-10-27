package dev.vladeezyy.kilnwork.fluid.api;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The fluid handler of a bucket item: one bucket (1000 mB) or nothing.
 */
public class FluidBucketWrapper implements IFluidHandlerItem {

    public static final int BUCKET_VOLUME = FluidStack.BUCKET_VOLUME;

    protected ItemStack container;

    public FluidBucketWrapper(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    public boolean canFillFluidType(FluidStack fluid) {
        return !fluid.getFluid().getBucket().getDefaultInstance().isEmpty() && fluid.getFluid().getBucket() != Items.AIR;
    }

    public FluidStack getFluid() {
        if (container.getItem() instanceof BucketItem bucketItem) {
            Fluid fluid = bucketItem.content;
            return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, BUCKET_VOLUME);
        }
        return FluidStack.EMPTY;
    }

    protected void setFluid(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            container = new ItemStack(Items.BUCKET);
        } else {
            container = new ItemStack(fluidStack.getFluid().getBucket());
        }
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
        return BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (container.getCount() != 1 || resource.getAmount() < BUCKET_VOLUME || !getFluid().isEmpty() || !canFillFluidType(resource)) {
            return 0;
        }
        if (action.execute()) {
            setFluid(resource);
        }
        return BUCKET_VOLUME;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (container.getCount() != 1 || resource.getAmount() < BUCKET_VOLUME) {
            return FluidStack.EMPTY;
        }
        FluidStack fluidStack = getFluid();
        if (!fluidStack.isEmpty() && FluidStack.isSameFluidSameComponents(fluidStack, resource)) {
            if (action.execute()) {
                setFluid(FluidStack.EMPTY);
            }
            return fluidStack;
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (container.getCount() != 1 || maxDrain < BUCKET_VOLUME) {
            return FluidStack.EMPTY;
        }
        FluidStack fluidStack = getFluid();
        if (!fluidStack.isEmpty()) {
            if (action.execute()) {
                setFluid(FluidStack.EMPTY);
            }
            return fluidStack;
        }
        return FluidStack.EMPTY;
    }
}
