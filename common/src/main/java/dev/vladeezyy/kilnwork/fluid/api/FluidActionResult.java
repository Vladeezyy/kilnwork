package dev.vladeezyy.kilnwork.fluid.api;

import net.minecraft.world.item.ItemStack;

/**
 * The outcome of a fluid container interaction: whether it worked and the resulting container.
 */
public class FluidActionResult {
    public static final FluidActionResult FAILURE = new FluidActionResult(false, ItemStack.EMPTY);

    public final boolean success;
    public final ItemStack result;

    public FluidActionResult(ItemStack result) {
        this(true, result);
    }

    private FluidActionResult(boolean success, ItemStack result) {
        this.success = success;
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public ItemStack getResult() {
        return result;
    }
}
