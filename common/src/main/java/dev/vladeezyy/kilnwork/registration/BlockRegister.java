package dev.vladeezyy.kilnwork.registration;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import dev.vladeezyy.kilnwork.platform.Registrar;
import net.minecraft.core.registries.Registries;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers blocks together with their items. Since 1.21.2 block and item properties carry their id, so blocks are
 * created from a properties supplier and items from a factory taking the block and the item properties.
 */
public class BlockRegister {
  private final Registrar<Block> blocks;
  private final Registrar<Item> items;

  /** The blocks and items are registered with the other registrars of the mod (see {@link Registrar}) */
  public BlockRegister(String modId) {
    this.blocks = Registrar.create(Registries.BLOCK, modId);
    this.items = Registrar.create(Registries.ITEM, modId);
  }

  /** Item register used for the block items, also usable for other items */
  public Registrar<Item> items() {
    return items;
  }

  /** Renames a block and its item, so worlds with the old id still load */
  public void addAlias(String from, String to) {
    blocks.addAlias(from, to);
    items.addAlias(from, to);
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
  public <B extends Block> Registrar.Entry<Block, B> registerNoItem(String name, Function<BlockBehaviour.Properties, ? extends B> block, Supplier<BlockBehaviour.Properties> properties) {
    return blocks.registerBlock(name, block, properties);
  }

  /** Registers a block and its item */
  public <B extends Block> Registrar.Entry<Block, B> register(String name, Function<BlockBehaviour.Properties, ? extends B> block, Supplier<BlockBehaviour.Properties> properties, BlockItemFactory<? super B> item) {
    Registrar.Entry<Block, B> holder = blocks.registerBlock(name, block, properties);
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
    Registrar.Entry<Block, Block> block = register(name, Block::new, properties, item);
    Registrar.Entry<Block, SlabBlock> slab = register(name + "_slab", SlabBlock::new, properties, item);
    Registrar.Entry<Block, StairBlock> stairs = register(name + "_stairs", props -> new StairBlock(block.get().defaultBlockState(), props), properties, item);
    Registrar.Entry<Block, WallBlock> wall = register(name + "_wall", WallBlock::new, properties, item);
    return new BuildingBlocks(block, slab, stairs, wall);
  }
}
