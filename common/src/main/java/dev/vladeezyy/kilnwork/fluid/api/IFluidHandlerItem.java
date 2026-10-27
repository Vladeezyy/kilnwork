package dev.vladeezyy.kilnwork.fluid.api;

import net.minecraft.world.item.ItemStack;

/**
 * A fluid handler of an item stack; {@link #getContainer()} is the (possibly replaced) container after changes.
 */
public interface IFluidHandlerItem extends IFluidHandler {
    ItemStack getContainer();
}
