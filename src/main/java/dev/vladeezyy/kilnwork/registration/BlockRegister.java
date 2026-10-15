package dev.vladeezyy.kilnwork.registration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers blocks together with their items
 */
public class BlockRegister {
  private final DeferredRegister.Blocks blocks;
  private final DeferredRegister.Items items;

  public BlockRegister(String modId) {
    this.blocks = DeferredRegister.createBlocks(modId);
    this.items = DeferredRegister.createItems(modId);
  }

  public void register(IEventBus bus) {
    blocks.register(bus);
    items.register(bus);
  }

  /** Item register used for the block items, also usable for other items */
  public DeferredRegister.Items items() {
    return items;
  }

  /** Renames a block and its item, so worlds with the old id still load */
  public void addAlias(String from, String to) {
    ResourceLocation fromId = ResourceLocation.fromNamespaceAndPath(blocks.getNamespace(), from);
    ResourceLocation toId = ResourceLocation.fromNamespaceAndPath(blocks.getNamespace(), to);
    blocks.addAlias(fromId, toId);
    items.addAlias(fromId, toId);
  }

  /** Registers a block without an item */
  public <B extends Block> DeferredBlock<B> registerNoItem(String name, Supplier<? extends B> block) {
    return blocks.register(name, block);
  }

  /** Registers a block and its item */
  public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> block, Function<? super B, ? extends BlockItem> item) {
    DeferredBlock<B> holder = blocks.register(name, block);
    items.register(name, () -> item.apply(holder.get()));
    return holder;
  }

  /** Registers a plain block from properties and its item */
  public DeferredBlock<Block> register(String name, BlockBehaviour.Properties props, Function<? super Block, ? extends BlockItem> item) {
    return register(name, () -> new Block(props), item);
  }

  /** Registers one block per enum value, named {@code <value>_<name>} */
  public <E extends Enum<E>, B extends Block> EnumObject<E, B> registerEnum(E[] values, String name, Function<E, ? extends B> block, Function<? super B, ? extends BlockItem> item) {
    EnumObject.Builder<E, B> builder = EnumObject.builder(values[0].getDeclaringClass());
    for (E value : values) {
      builder.put(value, this.<B>register(value.name().toLowerCase(Locale.ROOT) + '_' + name, () -> block.apply(value), item));
    }
    return builder.build();
  }

  /** Registers a block with slab ({@code _slab}), stairs ({@code _stairs}) and wall ({@code _wall}) */
  public BuildingBlocks registerBuilding(String name, BlockBehaviour.Properties props, Function<? super Block, ? extends BlockItem> item) {
    DeferredBlock<Block> block = register(name, () -> new Block(props), item);
    DeferredBlock<SlabBlock> slab = register(name + "_slab", () -> new SlabBlock(props), item);
    DeferredBlock<StairBlock> stairs = register(name + "_stairs", () -> new StairBlock(block.get().defaultBlockState(), props), item);
    DeferredBlock<WallBlock> wall = register(name + "_wall", () -> new WallBlock(props), item);
    return new BuildingBlocks(block, slab, stairs, wall);
  }

  /** Default block item */
  public static BlockItem blockItem(Block block) {
    return new BlockItem(block, new Item.Properties());
  }
}
