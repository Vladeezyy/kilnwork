package dev.vladeezyy.kilnwork.inventory;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.client.KilnworkClient;
import dev.vladeezyy.kilnwork.util.BlockEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Menu of a block entity with the player inventory as the last slots and shift click between the two.
 * Based on Mantle's BaseContainerMenu (MIT, SlimeKnights).
 */
public class BaseContainerMenu<T extends BlockEntity> extends AbstractContainerMenu {
  public static final int BASE_Y_OFFSET = 84;

  @Nullable
  protected final T tile;
  @Nullable
  protected final Inventory inv;
  protected int playerInventoryStart = -1;

  protected BaseContainerMenu(MenuType<?> type, int id, @Nullable Inventory inv, @Nullable T tile) {
    super(type, id);
    this.inv = inv;
    this.tile = tile;
  }

  @Nullable
  public T getTile() {
    return tile;
  }

  @Override
  public boolean stillValid(Player player) {
    if (tile == null) {
      return true;
    }
    if (tile.isRemoved()) {
      return false;
    }
    Level level = tile.getLevel();
    if (level == null || !level.isLoaded(tile.getBlockPos())) {
      return false;
    }
    return player.canInteractWithBlock(tile.getBlockPos(), 4.0);
  }

  /** Adds the player inventory, must be called after all other slots */
  protected void addInventorySlots() {
    if (inv != null) {
      addInventorySlots(inv);
    }
  }

  protected int getInventoryXOffset() {
    return 8;
  }

  protected int getInventoryYOffset() {
    return BASE_Y_OFFSET;
  }

  protected void addInventorySlots(Inventory inv) {
    int yOffset = getInventoryYOffset();
    int xOffset = getInventoryXOffset();
    int start = slots.size();
    for (int row = 0; row < 3; row++) {
      for (int column = 0; column < 9; column++) {
        addSlot(new Slot(inv, column + row * 9 + 9, xOffset + column * 18, yOffset + row * 18));
      }
    }
    yOffset += 58;
    for (int column = 0; column < 9; column++) {
      addSlot(new Slot(inv, column, xOffset + column * 18, yOffset));
    }
    playerInventoryStart = start;
  }

  @Override
  protected Slot addSlot(Slot slot) {
    if (playerInventoryStart >= 0) {
      throw new IllegalStateException("The player inventory has to be the last slots of the menu");
    }
    return super.addSlot(slot);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (playerInventoryStart < 0 || index < 0 || index >= slots.size()) {
      return ItemStack.EMPTY;
    }
    ItemStack result = ItemStack.EMPTY;
    Slot slot = slots.get(index);
    if (slot.hasItem()) {
      ItemStack stack = slot.getItem();
      result = stack.copy();
      if (index < playerInventoryStart) {
        if (!moveItemStackTo(stack, playerInventoryStart, slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (!moveItemStackTo(stack, 0, playerInventoryStart, false)) {
        return ItemStack.EMPTY;
      }
      if (stack.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }
    return result;
  }

  /** Vanilla merging ignores {@link Slot#getMaxStackSize(ItemStack)} when filling empty slots */
  @Override
  protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
    boolean moved = refill(stack, start, end, reverse);
    if (!stack.isEmpty()) {
      moved |= moveToEmpty(stack, start, end, reverse);
    }
    return moved;
  }

  private boolean refill(ItemStack stack, int start, int end, boolean reverse) {
    if (!stack.isStackable()) {
      return false;
    }
    boolean moved = false;
    for (int i = reverse ? end - 1 : start; !stack.isEmpty() && (reverse ? i >= start : i < end); i += reverse ? -1 : 1) {
      Slot slot = slots.get(i);
      ItemStack current = slot.getItem();
      if (!current.isEmpty() && ItemStack.isSameItemSameComponents(stack, current) && slot.mayPlace(stack)) {
        int limit = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
        int total = current.getCount() + stack.getCount();
        if (total <= limit) {
          stack.setCount(0);
          current.setCount(total);
          slot.setChanged();
          moved = true;
        } else if (current.getCount() < limit) {
          stack.shrink(limit - current.getCount());
          current.setCount(limit);
          slot.setChanged();
          moved = true;
        }
      }
    }
    return moved;
  }

  private boolean moveToEmpty(ItemStack stack, int start, int end, boolean reverse) {
    boolean moved = false;
    for (int i = reverse ? end - 1 : start; reverse ? i >= start : i < end; i += reverse ? -1 : 1) {
      Slot slot = slots.get(i);
      if (slot.getItem().isEmpty() && slot.mayPlace(stack)) {
        int limit = slot.getMaxStackSize(stack);
        ItemStack placed = stack.split(Math.min(limit, stack.getCount()));
        slot.set(placed);
        slot.setChanged();
        moved = true;
        if (stack.isEmpty()) {
          break;
        }
      }
    }
    return moved;
  }

  /**
   * Block entity of a client menu from the position in the buffer ({@code player.openMenu(menu, pos)})
   * @return the block entity, or null if missing or of another class
   */
  @Nullable
  public static <T extends BlockEntity> T getTileEntityFromBuf(@Nullable FriendlyByteBuf buf, Class<T> type) {
    if (buf == null) {
      return null;
    }
    Level level = KilnworkClient.getLevel();
    if (level == null) {
      return null;
    }
    BlockPos pos = buf.readBlockPos();
    if (!BlockEntityHelper.isBlockLoaded(level, pos)) {
      Kilnwork.LOG.error("Menu tried to open a {} at the unloaded position {}", type.getSimpleName(), pos);
      return null;
    }
    BlockEntity be = level.getBlockEntity(pos);
    if (type.isInstance(be)) {
      return type.cast(be);
    }
    Kilnwork.LOG.error("Menu expected a {} at {}, found {}", type.getSimpleName(), pos, be == null ? "nothing" : be.getClass().getName());
    return null;
  }
}
