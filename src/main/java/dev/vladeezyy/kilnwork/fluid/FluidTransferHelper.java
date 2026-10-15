package dev.vladeezyy.kilnwork.fluid;

import dev.vladeezyy.kilnwork.Kilnwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** Moving fluid between held items and tanks */
public final class FluidTransferHelper {
  private FluidTransferHelper() {}

  /** Sound for emptying the given fluid into a tank */
  public static SoundEvent getEmptySound(FluidStack fluid) {
    SoundEvent sound = fluid.getFluidType().getSound(fluid, SoundActions.BUCKET_EMPTY);
    return sound != null ? sound : fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
  }

  /** Sound for filling an item from a tank */
  public static SoundEvent getFillSound(FluidStack fluid) {
    SoundEvent sound = fluid.getFluidType().getSound(fluid, SoundActions.BUCKET_FILL);
    return sound != null ? sound : fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
  }

  private static void message(Player player, String key, FluidStack fluid) {
    player.displayClientMessage(Component.translatable(Kilnwork.lang("block", key), FluidText.number(fluid.getAmount()), fluid.getHoverName()), true);
  }

  /**
   * Fills or drains the tank at the position with the held item: data transfers (bowls, sponges), then items with a
   * fluid handler (buckets, clay buckets, modded tanks).
   * @return  True if the held item takes part in fluid transfer, even if nothing moved (on the client nothing moves)
   */
  public static boolean interactWithTank(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    ItemStack held = player.getItemInHand(hand);
    if (held.isEmpty()) {
      return false;
    }
    ResourceHandler<FluidResource> tank = level.getCapability(Capabilities.Fluid.BLOCK, pos, hit.getDirection());
    if (tank == null || tank.size() <= 0) {
      return false;
    }

    // data driven transfers
    if (ItemFluidTransfers.INSTANCE.mayHaveTransfer(held)) {
      if (!level.isClientSide()) {
        ItemFluidTransfers.Result result = ItemFluidTransfers.INSTANCE.transfer(held, tank);
        if (result != null) {
          level.playSound(null, pos, result.filled() ? getFillSound(result.fluid()) : getEmptySound(result.fluid()), SoundSource.BLOCKS, 1.0F, 1.0F);
          message(player, result.filled() ? "tank.drained" : "tank.filled", result.fluid());
          player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result.stack()));
        }
      }
      return true;
    }

    // items with a fluid handler
    if (ItemAccess.forPlayerInteraction(player, hand).oneByOne().getCapability(Capabilities.Fluid.ITEM) != null) {
      if (!level.isClientSide()) {
        FluidUtil.interactWithFluidHandler(player, hand, pos, tank);
      }
      return true;
    }
    return false;
  }
}
