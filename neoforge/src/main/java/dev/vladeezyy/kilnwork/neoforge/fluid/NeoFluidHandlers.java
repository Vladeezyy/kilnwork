package dev.vladeezyy.kilnwork.neoforge.fluid;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Converts between Kilnwork's fluid API and NeoForge's, both ways */
public final class NeoFluidHandlers {
  private NeoFluidHandlers() {}

  public static FluidStack fromNeo(net.neoforged.neoforge.fluids.FluidStack stack) {
    return stack.isEmpty() ? FluidStack.EMPTY : new FluidStack(stack.getFluidHolder(), stack.getAmount(), stack.getComponentsPatch());
  }

  public static net.neoforged.neoforge.fluids.FluidStack toNeo(FluidStack stack) {
    return stack.isEmpty() ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : new net.neoforged.neoforge.fluids.FluidStack(stack.getFluidHolder(), stack.getAmount(), stack.getComponentsPatch());
  }

  public static IFluidHandler.FluidAction fromNeo(net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
    return action.execute() ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE;
  }

  public static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction toNeo(IFluidHandler.FluidAction action) {
    return action.execute() ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;
  }

  /** Kilnwork handler as a NeoForge capability */
  @Nullable
  public static net.neoforged.neoforge.fluids.capability.IFluidHandler toNeo(@Nullable IFluidHandler handler) {
    if (handler == null) {
      return null;
    }
    if (handler instanceof FromNeo from) {
      return from.handler;
    }
    return handler instanceof IFluidHandlerItem item ? new ToNeoItem(item) : new ToNeo(handler);
  }

  @Nullable
  public static net.neoforged.neoforge.fluids.capability.IFluidHandlerItem toNeo(@Nullable IFluidHandlerItem handler) {
    return handler == null ? null : new ToNeoItem(handler);
  }

  /** NeoForge capability (of any mod) as a Kilnwork handler */
  @Nullable
  public static IFluidHandler fromNeo(@Nullable net.neoforged.neoforge.fluids.capability.IFluidHandler handler) {
    if (handler == null) {
      return null;
    }
    if (handler instanceof ToNeo to) {
      return to.handler;
    }
    return new FromNeo(handler);
  }

  @Nullable
  public static IFluidHandlerItem fromNeo(@Nullable net.neoforged.neoforge.fluids.capability.IFluidHandlerItem handler) {
    return handler == null ? null : new FromNeoItem(handler);
  }

  private static class ToNeo implements net.neoforged.neoforge.fluids.capability.IFluidHandler {
    final IFluidHandler handler;

    ToNeo(IFluidHandler handler) {
      this.handler = handler;
    }

    @Override
    public int getTanks() {
      return handler.getTanks();
    }

    @Override
    public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
      return toNeo(handler.getFluidInTank(tank));
    }

    @Override
    public int getTankCapacity(int tank) {
      return handler.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
      return handler.isFluidValid(tank, fromNeo(stack));
    }

    @Override
    public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
      return handler.fill(fromNeo(resource), fromNeo(action));
    }

    @Override
    public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
      return toNeo(handler.drain(fromNeo(resource), fromNeo(action)));
    }

    @Override
    public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
      return toNeo(handler.drain(maxDrain, fromNeo(action)));
    }
  }

  private static class ToNeoItem extends ToNeo implements net.neoforged.neoforge.fluids.capability.IFluidHandlerItem {
    ToNeoItem(IFluidHandlerItem handler) {
      super(handler);
    }

    @Override
    public ItemStack getContainer() {
      return ((IFluidHandlerItem) handler).getContainer();
    }
  }

  private static class FromNeo implements IFluidHandler {
    final net.neoforged.neoforge.fluids.capability.IFluidHandler handler;

    FromNeo(net.neoforged.neoforge.fluids.capability.IFluidHandler handler) {
      this.handler = handler;
    }

    @Override
    public int getTanks() {
      return handler.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
      return fromNeo(handler.getFluidInTank(tank));
    }

    @Override
    public int getTankCapacity(int tank) {
      return handler.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
      return handler.isFluidValid(tank, toNeo(stack));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
      return handler.fill(toNeo(resource), toNeo(action));
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
      return fromNeo(handler.drain(toNeo(resource), toNeo(action)));
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
      return fromNeo(handler.drain(maxDrain, toNeo(action)));
    }
  }

  private static class FromNeoItem extends FromNeo implements IFluidHandlerItem {
    FromNeoItem(net.neoforged.neoforge.fluids.capability.IFluidHandlerItem handler) {
      super(handler);
    }

    @Override
    public ItemStack getContainer() {
      return ((net.neoforged.neoforge.fluids.capability.IFluidHandlerItem) handler).getContainer();
    }
  }
}
