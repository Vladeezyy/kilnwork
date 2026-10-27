package dev.vladeezyy.kilnwork.neoforge.fluid;

import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler.FluidAction;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Converts between Kilnwork's fluid API and NeoForge's transfer API (21.9+), both ways. Kilnwork handlers act at once;
 * inside a NeoForge transaction the change is undone through a snapshot journal if the transaction is aborted.
 */
public final class NeoFluidHandlers {
  private NeoFluidHandlers() {}

  public static FluidResource toResource(FluidStack stack) {
    return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack.getFluidHolder(), stack.getComponentsPatch());
  }

  public static FluidStack toStack(FluidResource resource, int amount) {
    return resource.isEmpty() || amount <= 0 ? FluidStack.EMPTY : new FluidStack(resource.typeHolder(), amount, resource.getComponentsPatch());
  }

  public static net.neoforged.neoforge.fluids.FluidStack toNeo(FluidStack stack) {
    return stack.isEmpty() ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : new net.neoforged.neoforge.fluids.FluidStack(stack.getFluidHolder(), stack.getAmount(), stack.getComponentsPatch());
  }

  /** Opens a transaction nested in the open one of this thread, or a root one */
  static Transaction open() {
    TransactionContext current = Transaction.getCurrentOpenedTransaction();
    return current == null ? Transaction.openRoot() : Transaction.open(current);
  }

  /* Kilnwork -> NeoForge */

  @Nullable
  public static ResourceHandler<FluidResource> toNeo(@Nullable IFluidHandler handler) {
    if (handler == null) {
      return null;
    }
    if (handler instanceof FromNeo from) {
      return from.handler;
    }
    return new ToNeo(handler);
  }

  /** An item's Kilnwork handler as the NeoForge capability of an item access: works on a copy, then exchanges the item */
  public static ResourceHandler<FluidResource> itemToNeo(ItemAccess access, Function<ItemStack, IFluidHandlerItem> factory) {
    return new ToNeoItem(access, factory);
  }

  /** Undo log of the changes made inside open transactions */
  private static class ToNeo extends SnapshotJournal<Integer> implements ResourceHandler<FluidResource> {
    final IFluidHandler handler;
    private final List<Runnable> undo = new ArrayList<>();

    ToNeo(IFluidHandler handler) {
      this.handler = handler;
    }

    @Override
    protected Integer createSnapshot() {
      return undo.size();
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
      for (int i = undo.size() - 1; i >= snapshot; i--) {
        undo.remove(i).run();
      }
    }

    @Override
    protected void onRootCommit(Integer originalState) {
      undo.clear();
    }

    @Override
    public int size() {
      return handler.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
      return toResource(handler.getFluidInTank(index));
    }

    @Override
    public long getAmountAsLong(int index) {
      return handler.getFluidInTank(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
      return handler.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
      return handler.isFluidValid(index, toStack(resource, 1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
      return insert(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
      FluidStack stack = toStack(resource, amount);
      if (stack.isEmpty() || handler.fill(stack, FluidAction.SIMULATE) <= 0) {
        return 0;
      }
      updateSnapshots(transaction);
      int filled = handler.fill(stack.copy(), FluidAction.EXECUTE);
      if (filled > 0) {
        FluidStack undoStack = stack.copyWithAmount(filled);
        undo.add(() -> handler.drain(undoStack.copy(), FluidAction.EXECUTE));
      }
      return filled;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
      return extract(resource, amount, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
      FluidStack stack = toStack(resource, amount);
      if (stack.isEmpty() || handler.drain(stack, FluidAction.SIMULATE).isEmpty()) {
        return 0;
      }
      updateSnapshots(transaction);
      FluidStack drained = handler.drain(stack.copy(), FluidAction.EXECUTE);
      if (!drained.isEmpty()) {
        FluidStack undoStack = drained.copy();
        undo.add(() -> handler.fill(undoStack.copy(), FluidAction.EXECUTE));
      }
      return drained.getAmount();
    }
  }

  private static class ToNeoItem implements ResourceHandler<FluidResource> {
    private final ItemAccess access;
    private final Function<ItemStack, IFluidHandlerItem> factory;

    ToNeoItem(ItemAccess access, Function<ItemStack, IFluidHandlerItem> factory) {
      this.access = access;
      this.factory = factory;
    }

    @Nullable
    private IFluidHandlerItem handler() {
      ItemResource item = access.getResource();
      return item.isEmpty() || access.getAmount() <= 0 ? null : factory.apply(item.toStack(1));
    }

    @Override
    public int size() {
      IFluidHandlerItem handler = handler();
      return handler == null ? 0 : handler.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
      IFluidHandlerItem handler = handler();
      return handler == null ? FluidResource.EMPTY : toResource(handler.getFluidInTank(index));
    }

    @Override
    public long getAmountAsLong(int index) {
      IFluidHandlerItem handler = handler();
      return handler == null ? 0 : handler.getFluidInTank(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
      IFluidHandlerItem handler = handler();
      return handler == null ? 0 : handler.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
      IFluidHandlerItem handler = handler();
      return handler != null && handler.isFluidValid(index, toStack(resource, 1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
      IFluidHandlerItem handler = handler();
      FluidStack stack = toStack(resource, amount);
      if (handler == null || stack.isEmpty()) {
        return 0;
      }
      int filled = handler.fill(stack, FluidAction.EXECUTE);
      return filled > 0 && access.exchange(ItemResource.of(handler.getContainer()), 1, transaction) == 1 ? filled : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
      IFluidHandlerItem handler = handler();
      FluidStack stack = toStack(resource, amount);
      if (handler == null || stack.isEmpty()) {
        return 0;
      }
      FluidStack drained = handler.drain(stack, FluidAction.EXECUTE);
      return !drained.isEmpty() && access.exchange(ItemResource.of(handler.getContainer()), 1, transaction) == 1 ? drained.getAmount() : 0;
    }
  }

  /* NeoForge -> Kilnwork */

  @Nullable
  public static IFluidHandler fromNeo(@Nullable ResourceHandler<FluidResource> handler) {
    if (handler == null) {
      return null;
    }
    if (handler instanceof ToNeo to) {
      return to.handler;
    }
    return new FromNeo(handler);
  }

  /** The fluid handler of an item stack of any mod, on a private copy of the stack */
  @Nullable
  public static IFluidHandlerItem itemFromNeo(ItemStack stack) {
    StackAccess access = new StackAccess(stack.copy());
    ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
    return handler == null ? null : new FromNeoItem(handler, access);
  }

  private static class FromNeo implements IFluidHandler {
    final ResourceHandler<FluidResource> handler;

    FromNeo(ResourceHandler<FluidResource> handler) {
      this.handler = handler;
    }

    @Override
    public int getTanks() {
      return handler.size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
      return toStack(handler.getResource(tank), handler.getAmountAsInt(tank));
    }

    @Override
    public int getTankCapacity(int tank) {
      return handler.getCapacityAsInt(tank, handler.getResource(tank));
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
      return handler.isValid(tank, toResource(stack));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
      if (resource.isEmpty()) {
        return 0;
      }
      try (Transaction tx = open()) {
        int filled = handler.insert(toResource(resource), resource.getAmount(), tx);
        if (action.execute()) {
          tx.commit();
        }
        return filled;
      }
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
      if (resource.isEmpty()) {
        return FluidStack.EMPTY;
      }
      try (Transaction tx = open()) {
        int drained = handler.extract(toResource(resource), resource.getAmount(), tx);
        if (action.execute()) {
          tx.commit();
        }
        return resource.copyWithAmount(drained);
      }
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
      for (int i = 0; i < handler.size(); i++) {
        FluidResource resource = handler.getResource(i);
        if (!resource.isEmpty() && handler.getAmountAsInt(i) > 0) {
          try (Transaction tx = open()) {
            int drained = handler.extract(i, resource, maxDrain, tx);
            if (drained > 0) {
              if (action.execute()) {
                tx.commit();
              }
              return toStack(resource, drained);
            }
          }
        }
      }
      return FluidStack.EMPTY;
    }
  }

  private static class FromNeoItem extends FromNeo implements IFluidHandlerItem {
    private final StackAccess access;

    FromNeoItem(ResourceHandler<FluidResource> handler, StackAccess access) {
      super(handler);
      this.access = access;
    }

    @Override
    public ItemStack getContainer() {
      return access.stack.copy();
    }
  }

  /** An item access over one private stack, so the result of a transfer can be read back */
  private static class StackAccess extends SnapshotJournal<ItemStack> implements ItemAccess {
    ItemStack stack;

    StackAccess(ItemStack stack) {
      this.stack = stack;
    }

    @Override
    protected ItemStack createSnapshot() {
      return stack.copy();
    }

    @Override
    protected void revertToSnapshot(ItemStack snapshot) {
      stack = snapshot;
    }

    @Override
    public ItemResource getResource() {
      return ItemResource.of(stack);
    }

    @Override
    public int getAmount() {
      return stack.getCount();
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
      if (resource.isEmpty() || amount <= 0 || (!stack.isEmpty() && !resource.matches(stack))) {
        return 0;
      }
      int max = resource.toStack(1).getMaxStackSize();
      int inserted = Math.min(amount, max - (stack.isEmpty() ? 0 : stack.getCount()));
      if (inserted > 0) {
        updateSnapshots(transaction);
        stack = stack.isEmpty() ? resource.toStack(inserted) : stack.copyWithCount(stack.getCount() + inserted);
      }
      return Math.max(inserted, 0);
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
      if (resource.isEmpty() || amount <= 0 || stack.isEmpty() || !resource.matches(stack)) {
        return 0;
      }
      int extracted = Math.min(amount, stack.getCount());
      updateSnapshots(transaction);
      stack = stack.copyWithCount(stack.getCount() - extracted);
      return extracted;
    }
  }
}
