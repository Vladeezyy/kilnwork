package dev.vladeezyy.kilnwork.registration;

import net.minecraft.resources.Identifier;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers blocks together with their items. Since 1.21.2 block and item properties carry their id, so blocks are
 * created from a properties supplier and items from a factory taking the block and the item properties.
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
    Identifier fromId = Identifier.fromNamespaceAndPath(blocks.getNamespace(), from);
    Identifier toId = Identifier.fromNamespaceAndPath(blocks.getNamespace(), to);
    blocks.addAlias(fromId, toId);
    items.addAlias(fromId, toId);
  }

  /** Creates the item of a block */
  @FunctionalInterface
  public interface BlockItemFactory<B extends Block> {
    BlockItem create(B block, Item.Properties properties);
  }

  /** Default block item */
  public static BlockItem blockItem(Block block, Item.Properties properties) {
    return new BlockItem(block, properties);
  }

  /** Registers a block without an item */
  public <B extends Block> DeferredBlock<B> registerNoItem(String name, Function<BlockBehaviour.Properties, ? extends B> block, Supplier<BlockBehaviour.Properties> properties) {
    return blocks.registerBlock(name, block, properties);
  }

  /** Registers a block and its item */
  public <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, ? extends B> block, Supplier<BlockBehaviour.Properties> properties, BlockItemFactory<? super B> item) {
    DeferredBlock<B> holder = blocks.registerBlock(name, block, properties);
    items.registerItem(name, props -> item.create(holder.get(), props.useBlockDescriptionPrefix()), Item.Properties::new);
    return holder;
  }

  /** Registers one block per enum value, named {@code <value>_<name>} */
  public <E extends Enum<E>, B extends Block> EnumObject<E, B> registerEnum(E[] values, String name, BiFunction<E, BlockBehaviour.Properties, ? extends B> block, Function<E, BlockBehaviour.Properties> properties, BlockItemFactory<? super B> item) {
    EnumObject.Builder<E, B> builder = EnumObject.builder(values[0].getDeclaringClass());
    for (E value : values) {
      builder.put(value, this.<B>register(value.name().toLowerCase(Locale.ROOT) + '_' + name, props -> block.apply(value, props), () -> properties.apply(value), item));
    }
    return builder.build();
  }

  /** Registers a block with slab ({@code _slab}), stairs ({@code _stairs}) and wall ({@code _wall}) */
  public BuildingBlocks registerBuilding(String name, Supplier<BlockBehaviour.Properties> properties, BlockItemFactory<Block> item) {
    DeferredBlock<Block> block = register(name, Block::new, properties, item);
    DeferredBlock<SlabBlock> slab = register(name + "_slab", SlabBlock::new, properties, item);
    DeferredBlock<StairBlock> stairs = register(name + "_stairs", props -> new StairBlock(block.get().defaultBlockState(), props), properties, item);
    DeferredBlock<WallBlock> wall = register(name + "_wall", WallBlock::new, properties, item);
    return new BuildingBlocks(block, slab, stairs, wall);
  }
}
