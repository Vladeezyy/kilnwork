package dev.vladeezyy.kilnwork.plugin.jei;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.retexture.ShapedRetexturedRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/** Shows {@link ShapedRetexturedRecipe} with every texture, linked to the texture slots */
@JeiPlugin
public class KilnworkJEIPlugin implements IModPlugin {
  @Override
  public Identifier getPluginUid() {
    return Kilnwork.id("jei");
  }

  @Override
  public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
    registration.getCraftingCategory().addExtension(ShapedRetexturedRecipe.class, new RetexturedExtension());
  }

  /** Based on Mantle's RetexturableRecipeExtension (MIT, SlimeKnights) */
  private static class RetexturedExtension implements ICraftingCategoryExtension<ShapedRetexturedRecipe> {
    @Override
    public List<net.minecraft.world.item.crafting.display.SlotDisplay> getIngredients(RecipeHolder<ShapedRetexturedRecipe> holder) {
      return holder.value().getIngredients().stream()
        .map(ingredient -> ingredient.map(net.minecraft.world.item.crafting.Ingredient::display).orElse(net.minecraft.world.item.crafting.display.SlotDisplay.Empty.INSTANCE))
        .toList();
    }

    @Override
    public int getWidth(RecipeHolder<ShapedRetexturedRecipe> holder) {
      return holder.value().getWidth();
    }

    @Override
    public int getHeight(RecipeHolder<ShapedRetexturedRecipe> holder) {
      return holder.value().getHeight();
    }

    @Override
    public void setRecipe(RecipeHolder<ShapedRetexturedRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
      ShapedRetexturedRecipe recipe = holder.value();
      // the untextured result, so a lookup of the plain item finds the recipe too
      builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.getPlainOutput());
      List<List<ItemStack>> inputs = recipe.getIngredients().stream()
        .map(ingredient -> ingredient.map(i -> i.items().map(ItemStack::new).toList()).orElse(List.of())).toList();
      int width = recipe.getWidth();
      int height = recipe.getHeight();
      List<IRecipeSlotBuilder> slots = grid.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, width, height);
      IRecipeSlotBuilder output = grid.createAndSetOutputs(builder, recipe.getDisplayOutputs());
      if (slots.size() != 9) {
        Kilnwork.LOG.error("Cannot link the texture slots of {}, the crafting grid is not 3x3", holder.id());
        return;
      }
      List<IRecipeSlotBuilder> linked = new ArrayList<>();
      linked.add(output);
      for (int slot : recipe.getTextureSlots()) {
        linked.add(slots.get(gridIndex(slot, width, height)));
      }
      builder.createFocusLink(linked.toArray(IRecipeSlotBuilder[]::new));
    }
  }

  /** Index in JEI's 3x3 grid of a recipe slot, as JEI's CraftingGridHelper places them */
  static int gridIndex(int i, int width, int height) {
    if (width == 1) {
      return height == 1 ? 4 : i * 3 + 1;
    }
    if (height == 1) {
      return i + 3;
    }
    if (width == 2) {
      int index = i;
      if (i > 1) {
        index++;
        if (i > 3) {
          index++;
        }
      }
      return index;
    }
    if (height == 2) {
      return i + 3;
    }
    return i;
  }
}
