package dev.vladeezyy.kilnwork.fluid.api;

import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;

/**
 * A single fluid tank.
 */
public interface IFluidTank {

    FluidStack getFluid();

    int getFluidAmount();

    int getCapacity();

    boolean isFluidValid(FluidStack stack);

    int fill(FluidStack resource, IFluidHandler.FluidAction action);

    FluidStack drain(int maxDrain, IFluidHandler.FluidAction action);

    FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action);
}
