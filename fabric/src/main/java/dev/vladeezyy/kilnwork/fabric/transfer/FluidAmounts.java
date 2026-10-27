package dev.vladeezyy.kilnwork.fabric.transfer;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/**
 * Conversions between millibuckets and Fabric droplets (81 per millibucket).
 */
public final class FluidAmounts {

    public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private FluidAmounts() {
    }

    public static FluidVariant toVariant(FluidStack stack) {
        return stack.isEmpty() ? FluidVariant.blank() : FluidVariant.of(stack.getFluid(), stack.getComponentsPatch());
    }

    public static FluidStack fromVariant(FluidVariant variant, int amount) {
        if (variant.isBlank() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(variant.getFluid().builtInRegistryHolder(), amount, variant.getComponentsPatch());
    }

    public static long toDroplets(int mb) {
        return mb * DROPLETS_PER_MB;
    }

    /**
     * Whole millibuckets in an amount of droplets, rounded down.
     */
    public static int toMb(long droplets) {
        return (int) Math.min(Integer.MAX_VALUE, droplets / DROPLETS_PER_MB);
    }

    /**
     * Opens a transaction that is nested in the transaction of this thread, if there is one.
     */
    public static Transaction open() {
        return Transaction.openNested(Transaction.getCurrentUnsafe());
    }
}
