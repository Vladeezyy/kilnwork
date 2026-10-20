package dev.vladeezyy.kilnwork.retexture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vladeezyy.kilnwork.KilnworkRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Shaped recipe giving the result the texture of the item matching {@code texture}. JSON as in Mantle 1.20:
 * shaped recipe fields plus {@code "texture": <ingredient>} and {@code "match_all"} (all texture items must be the
 * same block). Based on Mantle's ShapedRetexturedRecipe (MIT, SlimeKnights).
 */
public class ShapedRetexturedRecipe extends ShapedRecipe {
  public static final MapCodec<ShapedRetexturedRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
    ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
    ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.plainResult),
    Ingredient.CODEC.fieldOf("texture").forGetter(r -> r.texture),
    Codec.BOOL.optionalFieldOf("match_all", false).forGetter(r -> r.matchAll)
  ).apply(inst, ShapedRetexturedRecipe::new));
  public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRetexturedRecipe> STREAM_CODEC = StreamCodec.composite(
    Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
    CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
    ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
    ItemStackTemplate.STREAM_CODEC, r -> r.plainResult,
    Ingredient.CONTENTS_STREAM_CODEC, r -> r.texture,
    ByteBufCodecs.BOOL, r -> r.matchAll,
    ShapedRetexturedRecipe::new);
  public static final RecipeSerializer<ShapedRetexturedRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

  private final ItemStackTemplate plainResult;
  private final Ingredient texture;
  private final boolean matchAll;
  private List<ItemStack> displayOutputs;
  private int[] textureSlots;

  public ShapedRetexturedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, ItemStackTemplate result, Ingredient texture, boolean matchAll) {
    super(commonInfo, bookInfo, pattern, result);
    this.plainResult = result;
    this.texture = texture;
    this.matchAll = matchAll;
  }

  /** Ingredient giving the texture */
  public Ingredient getTexture() {
    return texture;
  }

  /** If true, all items matching the texture must be the same block */
  public boolean isMatchAll() {
    return matchAll;
  }

  /** Result without a texture */
  public ItemStack getPlainOutput() {
    return plainResult.create();
  }

  @Override
  public ItemStack assemble(CraftingInput input) {
    ItemStack result = super.assemble(input);
    Block current = null;
    for (int i = 0; i < input.size(); i++) {
      ItemStack stack = input.getItem(i);
      if (stack.isEmpty() || !texture.test(stack)) {
        continue;
      }
      // a retextured input passes its texture on, any other block item is the texture itself
      Block block = RetexturedHelper.getTexture(stack);
      if (block == Blocks.AIR && stack.getItem() != result.getItem()) {
        block = Block.byItem(stack.getItem());
      }
      if (block == Blocks.AIR) {
        continue;
      }
      if (current == null) {
        current = block;
        if (!matchAll) {
          break;
        }
      } else if (current != block) {
        current = null;
        break;
      }
    }
    return current != null ? RetexturedHelper.setTexture(result, current) : result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public RecipeSerializer<ShapedRecipe> getSerializer() {
    // the serializer makes this subclass, vanilla's return type names the parent
    return (RecipeSerializer<ShapedRecipe>)(RecipeSerializer<?>)KilnworkRegistries.SHAPED_RETEXTURED.get();
  }

  /** Every textured result, for recipe viewers */
  public List<ItemStack> getDisplayOutputs() {
    if (displayOutputs == null) {
      displayOutputs = texture.items()
        .map(holder -> Block.byItem(holder.value()))
        .filter(block -> block != Blocks.AIR)
        .distinct()
        .map(block -> RetexturedHelper.setTexture(plainResult.create(), block))
        .toList();
      if (displayOutputs.isEmpty()) {
        displayOutputs = List.of(plainResult.create());
      }
    }
    return displayOutputs;
  }

  /** Indices of the pattern holding the texture ingredient */
  public int[] getTextureSlots() {
    if (textureSlots == null) {
      List<java.util.Optional<Ingredient>> inputs = getIngredients();
      textureSlots = IntStream.range(0, inputs.size()).filter(i -> inputs.get(i).filter(texture::equals).isPresent()).toArray();
    }
    return textureSlots;
  }
}
