package dev.vladeezyy.kilnwork.registration;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

/**
 * A full block with its slab, stairs and wall
 * @param block   Full block
 * @param slab    Slab
 * @param stairs  Stairs
 * @param wall    Wall
 */
public record BuildingBlocks(Supplier<? extends Block> block, Supplier<? extends Block> slab, Supplier<? extends Block> stairs, Supplier<? extends Block> wall) implements ItemLike {
  public Block get() {
    return block.get();
  }

  public SlabBlock getSlab() {
    return (SlabBlock) slab.get();
  }

  public StairBlock getStairs() {
    return (StairBlock) stairs.get();
  }

  public WallBlock getWall() {
    return (WallBlock) wall.get();
  }

  /** All four blocks */
  public List<Block> values() {
    return List.of(get(), getSlab(), getStairs(), getWall());
  }

  @Override
  public Item asItem() {
    return get().asItem();
  }
}
