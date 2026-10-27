package dev.vladeezyy.kilnwork.retexture;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.KilnworkRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Texture of retextured blocks: the {@code kilnwork:texture} component on items and a {@code texture} string in block
 * entity data. Based on Mantle's RetexturedHelper (MIT, SlimeKnights).
 */
public final class RetexturedHelper {
  private RetexturedHelper() {}

  /** Translation key for the texture id in advanced tooltips */
  public static final String KEY_ID = Kilnwork.lang("block", "retextured.id");
  /** Key of the texture in block entity data, and in item data of 1.20 worlds */
  public static final String TAG_TEXTURE = "texture";
  /** Model property holding the texture block */
  public static final ModelProperty<Block> BLOCK_PROPERTY = new ModelProperty<>(block -> block != Blocks.AIR);

  /** Name of the texture block, empty for air */
  public static String getTextureName(Block block) {
    if (block == Blocks.AIR) {
      return "";
    }
    return BuiltInRegistries.BLOCK.getKey(block).toString();
  }

  /** Block for the given name, air if missing or invalid */
  public static Block getBlock(String name) {
    if (!name.isEmpty()) {
      ResourceLocation location = ResourceLocation.tryParse(name);
      if (location != null) {
        return BuiltInRegistries.BLOCK.getOptional(location).orElse(Blocks.AIR);
      }
    }
    return Blocks.AIR;
  }

  /**
   * Texture of a stack, air if none. Stacks from 1.20 worlds keep the texture as {@code texture} in
   * {@code minecraft:custom_data}, that is read too.
   */
  public static Block getTexture(ItemStack stack) {
    Block block = stack.get(KilnworkRegistries.TEXTURE);
    if (block != null) {
      return block;
    }
    return getLegacyTexture(stack);
  }

  /** Texture kept by a 1.20 world in custom data, air if none */
  public static Block getLegacyTexture(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      CompoundTag tag = data.getUnsafe();
      if (tag.contains(TAG_TEXTURE, Tag.TAG_STRING)) {
        return getBlock(tag.getString(TAG_TEXTURE));
      }
    }
    return Blocks.AIR;
  }

  /**
   * Moves a texture of a 1.20 world from custom data to the component.
   * @return true if the stack changed
   */
  public static boolean convertLegacyTexture(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null || !data.contains(TAG_TEXTURE)) {
      return false;
    }
    Block block = getBlock(data.copyTag().getString(TAG_TEXTURE));
    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(TAG_TEXTURE));
    if (block != Blocks.AIR && !stack.has(KilnworkRegistries.TEXTURE)) {
      stack.set(KilnworkRegistries.TEXTURE, block);
    }
    return true;
  }

  /** Sets the texture of the stack, air or null removes it */
  public static ItemStack setTexture(ItemStack stack, @Nullable Block block) {
    if (block == null || block == Blocks.AIR) {
      stack.remove(KilnworkRegistries.TEXTURE);
    } else {
      stack.set(KilnworkRegistries.TEXTURE, block);
    }
    return stack;
  }

  /** Call on both sides when the texture of a block entity changes, refreshes the model on the client */
  public static void onTextureUpdated(BlockEntity self) {
    Level level = self.getLevel();
    if (level != null && level.isClientSide) {
      self.requestModelDataUpdate();
      BlockState state = self.getBlockState();
      level.sendBlockUpdated(self.getBlockPos(), state, state, 0);
    }
  }

  /** Model data builder with the texture, air is left out */
  public static ModelData.Builder getModelDataBuilder(Block block) {
    ModelData.Builder builder = ModelData.builder();
    if (block != Blocks.AIR) {
      builder.with(BLOCK_PROPERTY, block);
    }
    return builder;
  }

  /** Model data with the texture */
  public static ModelData getModelData(Block block) {
    if (block == Blocks.AIR) {
      return ModelData.EMPTY;
    }
    return getModelDataBuilder(block).build();
  }

  /** Adds the texture block, and its id in advanced tooltips */
  public static void addTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
    Block block = getTexture(stack);
    if (block != Blocks.AIR) {
      tooltip.add(block.getName().withStyle(ChatFormatting.GRAY));
      if (flag.isAdvanced()) {
        tooltip.add(Component.translatable(KEY_ID, BuiltInRegistries.BLOCK.getKey(block).toString()).withStyle(ChatFormatting.DARK_GRAY));
      }
    }
  }

  /**
   * Adds a variant of the block for every block item in the tag, for creative tabs
   * @param tab    Receives the stacks, returning true stops adding
   * @param block  Retextured block
   * @param tag    Tag of texture items
   * @return true if anything was added
   */
  public static boolean addTagVariants(Predicate<ItemStack> tab, ItemLike block, TagKey<Item> tag) {
    boolean added = false;
    for (Holder<Item> candidate : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (!candidate.isBound()) {
        continue;
      }
      Item item = candidate.value();
      // the block itself is no texture, see enlightened bushes
      if (item == block.asItem() || !(item instanceof BlockItem blockItem)) {
        continue;
      }
      added = true;
      if (tab.test(setTexture(new ItemStack(block), blockItem.getBlock()))) {
        break;
      }
    }
    return added;
  }

  /** Loot function copying the texture from the block entity (needs {@link IRetexturedBlockEntity#collectImplicitComponents}) */
  public static CopyComponentsFunction.Builder copyTexture() {
    return CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(KilnworkRegistries.TEXTURE.get());
  }
}
