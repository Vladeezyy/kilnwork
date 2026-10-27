package dev.vladeezyy.kilnwork.fluid.api;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;

/**
 * Something that holds fluids in one or more tanks, with amounts in millibuckets.
 */
public interface IFluidHandler {

    enum FluidAction {
        EXECUTE, SIMULATE;

        public boolean execute() {
            return this == EXECUTE;
        }

        public boolean simulate() {
            return this == SIMULATE;
        }
    }

    int getTanks();

    FluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    boolean isFluidValid(int tank, FluidStack stack);

    /**
     * @return The amount that was (or would be) filled.
     */
    int fill(FluidStack resource, FluidAction action);

    /**
     * Drain the given fluid (fluid and components must match).
     */
    FluidStack drain(FluidStack resource, FluidAction action);

    /**
     * Drain any fluid, up to the given amount.
     */
    FluidStack drain(int maxDrain, FluidAction action);
}
