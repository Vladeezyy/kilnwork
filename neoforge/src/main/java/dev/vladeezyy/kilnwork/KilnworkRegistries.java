package dev.vladeezyy.kilnwork;

import dev.vladeezyy.kilnwork.retexture.ShapedRetexturedRecipe;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry entries of Kilnwork itself */
public final class KilnworkRegistries {
  private KilnworkRegistries() {}

  private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Kilnwork.MOD_ID);
  private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Kilnwork.MOD_ID);

  /** Block whose texture a retextured block uses, {@code kilnwork:texture} */
  public static final DeferredHolder<DataComponentType<?>, DataComponentType<Block>> TEXTURE = COMPONENTS.registerComponentType("texture",
    builder -> builder.persistent(BuiltInRegistries.BLOCK.byNameCodec()).networkSynchronized(ByteBufCodecs.registry(Registries.BLOCK)).cacheEncoding());

  /** Shaped crafting that takes the texture of one ingredient, {@code kilnwork:crafting_shaped_retextured} */
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRetexturedRecipe>> SHAPED_RETEXTURED = RECIPE_SERIALIZERS.register("crafting_shaped_retextured",
    () -> ShapedRetexturedRecipe.SERIALIZER);

  public static void register(IEventBus bus) {
    COMPONENTS.register(bus);
    RECIPE_SERIALIZERS.register(bus);
  }
}
