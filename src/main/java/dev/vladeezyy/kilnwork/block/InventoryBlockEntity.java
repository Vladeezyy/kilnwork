package dev.vladeezyy.kilnwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * Block entity with a vanilla {@link Container} inventory and an item handler for it. Register the item handler
 * capability yourself ({@code RegisterCapabilitiesEvent}). Based on Mantle's InventoryBlockEntity (MIT, SlimeKnights).
 */
public abstract class InventoryBlockEntity extends NameableBlockEntity implements Container {
  private static final String TAG_ITEMS = "Items";
  private static final String TAG_SLOT = "Slot";

  private final NonNullList<ItemStack> inventory;
  protected final int stackSizeLimit;
  protected final IItemHandlerModifiable itemHandler;

  public InventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, int inventorySize) {
    this(type, pos, state, name, inventorySize, 64);
  }

  public InventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, int inventorySize, int maxStackSize) {
    super(type, pos, state, name);
    this.inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
    this.stackSizeLimit = maxStackSize;
    this.itemHandler = new InvWrapper(this);
  }

  /** Item handler over this inventory */
  public IItemHandlerModifiable getItemHandler() {
    return itemHandler;
  }

  @Override
  public ItemStack getItem(int slot) {
    if (slot < 0 || slot >= inventory.size()) {
      return ItemStack.EMPTY;
    }
    return inventory.get(slot);
  }

  @Override
  public boolean isEmpty() {
    for (ItemStack stack : inventory) {
      if (!stack.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int getContainerSize() {
    return inventory.size();
  }

  @Override
  public int getMaxStackSize() {
    return stackSizeLimit;
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    if (slot < 0 || slot >= inventory.size()) {
      return;
    }
    ItemStack current = inventory.get(slot);
    inventory.set(slot, stack);
    if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
      stack.setCount(getMaxStackSize());
    }
    if (!ItemStack.matches(current, stack)) {
      setChangedFast();
    }
  }

  @Override
  public ItemStack removeItem(int slot, int quantity) {
    if (quantity <= 0) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = getItem(slot);
    if (stack.isEmpty()) {
      return ItemStack.EMPTY;
    }
    if (stack.getCount() <= quantity) {
      setItem(slot, ItemStack.EMPTY);
      setChangedFast();
      return stack;
    }
    ItemStack split = stack.split(quantity);
    setChangedFast();
    return split;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    ItemStack stack = getItem(slot);
    setItem(slot, ItemStack.EMPTY);
    return stack;
  }

  @Override
  public void clearContent() {
    inventory.replaceAll(ignored -> ItemStack.EMPTY);
  }

  @Override
  public boolean stillValid(Player player) {
    if (level == null || level.getBlockEntity(worldPosition) != this || getBlockState().isAir()) {
      return false;
    }
    return player.canInteractWithBlock(worldPosition, 4.0);
  }

  @Override
  protected void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    readInventory(tags, registries);
  }

  @Override
  protected void saveAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveAdditional(tags, registries);
    writeInventory(tags, registries);
  }

  /** Writes the items as {@code Items: [{Slot: n, ...}]}, the format of Mantle 1.20 */
  public void writeInventory(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = new ListTag();
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.get(i);
      if (!stack.isEmpty()) {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putByte(TAG_SLOT, (byte) i);
        list.add(stack.save(registries, itemTag));
      }
    }
    tag.put(TAG_ITEMS, list);
  }

  /** Reads the items written by {@link #writeInventory}, replacing the contents */
  public void readInventory(CompoundTag tag, HolderLookup.Provider registries) {
    inventory.replaceAll(ignored -> ItemStack.EMPTY);
    ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag itemTag = list.getCompound(i);
      int slot = itemTag.getByte(TAG_SLOT) & 255;
      if (slot < inventory.size()) {
        ItemStack stack = ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY);
        if (stack.getCount() > stackSizeLimit) {
          stack.setCount(stackSizeLimit);
        }
        inventory.set(slot, stack);
      }
    }
  }
}
