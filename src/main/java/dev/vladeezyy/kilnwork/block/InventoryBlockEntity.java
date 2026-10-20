package dev.vladeezyy.kilnwork.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import dev.vladeezyy.kilnwork.util.LegacyItemStacks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/**
 * Block entity with a vanilla {@link Container} inventory and a transfer API handler for it. Register the item
 * capability yourself ({@code RegisterCapabilitiesEvent}, {@code Capabilities.Item.BLOCK}). Based on Mantle's InventoryBlockEntity (MIT, SlimeKnights).
 */
public abstract class InventoryBlockEntity extends NameableBlockEntity implements Container {
  private static final String TAG_ITEMS = "Items";
  private static final String TAG_SLOT = "Slot";

  private final NonNullList<ItemStack> inventory;
  protected final int stackSizeLimit;
  protected final ResourceHandler<ItemResource> itemHandler;

  public InventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, int inventorySize) {
    this(type, pos, state, name, inventorySize, 64);
  }

  public InventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, int inventorySize, int maxStackSize) {
    super(type, pos, state, name);
    this.inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
    this.stackSizeLimit = maxStackSize;
    this.itemHandler = VanillaContainerWrapper.of(this);
  }

  /** Item handler (transfer API) over this inventory */
  public ResourceHandler<ItemResource> getItemHandler() {
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
    return Container.stillValidBlockEntity(this, player);
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    readInventory(input);
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    writeInventory(output);
  }

  /** Writes the items as {@code Items: [{Slot: n, ...}]}, the format of Mantle 1.20 */
  public void writeInventory(ValueOutput output) {
    ValueOutput.TypedOutputList<ItemStackWithSlot> list = output.list(TAG_ITEMS, ItemStackWithSlot.CODEC);
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.get(i);
      if (!stack.isEmpty()) {
        list.add(new ItemStackWithSlot(i, stack));
      }
    }
  }

  /** Reads the items written by {@link #writeInventory}, replacing the contents; items of 1.20 worlds are converted */
  public void readInventory(ValueInput input) {
    inventory.replaceAll(ignored -> ItemStack.EMPTY);
    for (CompoundTag itemTag : input.read(TAG_ITEMS, CompoundTag.CODEC.listOf()).orElse(java.util.List.of())) {
      int slot = itemTag.getByteOr(TAG_SLOT, (byte) 0) & 255;
      if (slot < inventory.size()) {
        ItemStack stack = LegacyItemStacks.parse(input.lookup(), itemTag);
        if (stack.getCount() > stackSizeLimit) {
          stack.setCount(stackSizeLimit);
        }
        inventory.set(slot, stack);
      }
    }
  }
}
