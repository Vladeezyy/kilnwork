package dev.vladeezyy.kilnwork.fluid;

import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.Kilnwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler.FluidAction;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem;

/** Moving fluid between held items and tanks */
public final class FluidTransferHelper {
  private FluidTransferHelper() {}

  /** Sound for emptying the given fluid into a tank */
  public static SoundEvent getEmptySound(FluidStack fluid) {
    SoundEvent sound = KilnworkPlatform.get().fluidEmptySound(fluid);
    return sound != null ? sound : fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
  }

  /** Sound for filling an item from a tank */
  public static SoundEvent getFillSound(FluidStack fluid) {
    SoundEvent sound = KilnworkPlatform.get().fluidFillSound(fluid);
    return sound != null ? sound : fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
  }

  /**
   * Moves as much of the fluid as fits from input to output
   * @return  Fluid moved, empty if nothing
   */
  public static FluidStack tryTransfer(IFluidHandler input, IFluidHandler output, int maxFill) {
    FluidStack fluid = input.drain(maxFill, FluidAction.SIMULATE);
    if (!fluid.isEmpty()) {
      int filled = output.fill(fluid.copy(), FluidAction.SIMULATE);
      if (filled > 0) {
        FluidStack drained = input.drain(fluid.copyWithAmount(filled), FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
          int actual = output.fill(drained.copy(), FluidAction.EXECUTE);
          // could not fill everything that was drained, put the rest back
          if (actual < drained.getAmount()) {
            int rest = drained.getAmount() - actual;
            int returned = input.fill(drained.copyWithAmount(rest), FluidAction.EXECUTE);
            if (returned < rest) {
              Kilnwork.LOG.error("Lost {} mB of fluid during a transfer", rest - returned);
            }
            drained.setAmount(actual);
          }
        }
        return drained;
      }
    }
    return FluidStack.EMPTY;
  }

  private static void message(Player player, String key, FluidStack fluid) {
    player.sendOverlayMessage(Component.translatable(Kilnwork.lang("block", key), FluidText.number(fluid.getAmount()), fluid.getHoverName()));
  }

  /** Plays the sound and shows the message for filling a tank */
  public static void playEmptySound(Level level, BlockPos pos, Player player, FluidStack transferred) {
    level.playSound(null, pos, getEmptySound(transferred), SoundSource.BLOCKS, 1.0F, 1.0F);
    message(player, "tank.filled", transferred);
  }

  /** Plays the sound and shows the message for draining a tank */
  public static void playFillSound(Level level, BlockPos pos, Player player, FluidStack transferred) {
    level.playSound(null, pos, getFillSound(transferred), SoundSource.BLOCKS, 1.0F, 1.0F);
    message(player, "tank.drained", transferred);
  }

  /**
   * Fills or drains the tank at the position with the held item: data transfers (bowls, sponges), then fluid handler
   * items, then filled buckets without a fluid handler (mob buckets).
   * @return  True if the held item takes part in fluid transfer, even if nothing moved (on the client nothing moves)
   */
  public static boolean interactWithTank(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    Direction side = hit.getDirection();
    ItemStack held = player.getItemInHand(hand);
    if (held.isEmpty()) {
      return false;
    }
    IFluidHandler tank = KilnworkPlatform.get().fluidHandler(level, pos, side);
    if (tank == null || tank.getTanks() <= 0) {
      return false;
    }

    // data driven transfers
    if (ItemFluidTransfers.INSTANCE.mayHaveTransfer(held)) {
      if (!level.isClientSide()) {
        ItemFluidTransfers.Result result = ItemFluidTransfers.INSTANCE.transfer(held, tank);
        if (result != null) {
          if (result.filled()) {
            playFillSound(level, pos, player, result.fluid());
          } else {
            playEmptySound(level, pos, player, result.fluid());
          }
          player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result.stack()));
        }
      }
      return true;
    }

    // fluid handler items
    IFluidHandlerItem item = KilnworkPlatform.get().fluidHandler(held.copyWithCount(1));
    if (item != null) {
      if (!level.isClientSide()) {
        FluidStack moved = tryTransfer(item, tank, Integer.MAX_VALUE);
        if (!moved.isEmpty()) {
          playEmptySound(level, pos, player, moved);
        } else {
          moved = tryTransfer(tank, item, Integer.MAX_VALUE);
          if (!moved.isEmpty()) {
            playFillSound(level, pos, player, moved);
          }
        }
        if (!moved.isEmpty()) {
          player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, item.getContainer()));
        }
      }
      return true;
    }

    // buckets without a fluid handler, such as fish buckets
    if (held.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY) {
      if (!level.isClientSide()) {
        Fluid fluid = bucket.content;
        FluidStack stack = new FluidStack(fluid, FluidStack.BUCKET_VOLUME);
        if (tank.fill(stack, FluidAction.SIMULATE) == FluidStack.BUCKET_VOLUME) {
          tank.fill(stack.copy(), FluidAction.EXECUTE);
          bucket.checkExtraContent(player, level, held, pos.relative(side));
          playEmptySound(level, pos, player, stack);
          // mob buckets have no crafting remainder, so ask the bucket for its empty form
          player.setItemInHand(hand, BucketItem.getEmptySuccessItem(held, player));
        }
      }
      return true;
    }
    return false;
  }
}
