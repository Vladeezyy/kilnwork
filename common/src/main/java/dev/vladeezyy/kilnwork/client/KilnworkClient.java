package dev.vladeezyy.kilnwork.client;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.client.render.KilnworkFluidModels;
import dev.vladeezyy.kilnwork.client.render.RenderItem;
import dev.vladeezyy.kilnwork.fluid.FluidText;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.fluid.api.IFluidHandler;
import dev.vladeezyy.kilnwork.platform.ClientLoaderHooks;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import dev.vladeezyy.kilnwork.tag.KilnworkTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Client side of Kilnwork: data loaders and the gauge tooltip; the loader parts call {@link #init()} */
public final class KilnworkClient {
  private KilnworkClient() {}

  /** Client registrations of Kilnwork itself, the same on every loader */
  public static void init() {
    ClientLoaderHooks.reloadListener(Kilnwork.id("block_fluids"), KilnworkFluidModels.BLOCK_FLUIDS);
    ClientLoaderHooks.reloadListener(Kilnwork.id("faucet_fluids"), KilnworkFluidModels.FAUCET_FLUIDS);
    ClientLoaderHooks.reloadListener(Kilnwork.id("channel_fluids"), KilnworkFluidModels.CHANNEL_FLUIDS);
    ClientLoaderHooks.reloadListener(Kilnwork.id("item_lists"), RenderItem.ITEM_LISTS);
  }

  /** Current level on the client, null if not in a world */
  @Nullable
  public static Level getLevel() {
    return Minecraft.getInstance().level;
  }

  /** Shows the fluid of a gauge or tank tagged in {@link KilnworkTags.Blocks#GAUGES} next to the crosshair; called by the loader's HUD hook */
  public static void renderGaugeTooltip(GuiGraphics graphics) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.screen != null && minecraft.screen.getClass() != ChatScreen.class) {
      return;
    }
    if (minecraft.level == null || minecraft.options.hideGui || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
      return;
    }
    BlockPos pos = hit.getBlockPos();
    BlockState targeted = minecraft.level.getBlockState(pos);
    if (!targeted.is(KilnworkTags.Blocks.GAUGES)) {
      return;
    }
    BlockPos fluidPos = pos;
    BlockState fluidBlock = targeted;
    Direction side = hit.getDirection();
    if (targeted.is(KilnworkTags.Blocks.ATTACHED_GAUGES)) {
      if (targeted.hasProperty(BlockStateProperties.FACING)) {
        side = targeted.getValue(BlockStateProperties.FACING);
      }
      fluidPos = pos.relative(side.getOpposite());
      fluidBlock = minecraft.level.getBlockState(fluidPos);
    }
    if (fluidBlock.is(KilnworkTags.Blocks.GAUGE_BLACKLIST)) {
      return;
    }
    IFluidHandler handler = KilnworkPlatform.get().fluidHandler(minecraft.level, fluidPos, side);
    if (handler == null || handler.getTanks() <= 0) {
      return;
    }
    FluidStack fluid = handler.getFluidInTank(0);
    int capacity = handler.getTankCapacity(0);
    List<Component> tooltip = new ArrayList<>(3);
    if (fluid.isEmpty()) {
      tooltip.add(FluidText.capacity(capacity));
    } else {
      tooltip.add(fluid.getHoverName());
      if (!fluidBlock.is(KilnworkTags.Blocks.HIDES_GAUGE_AMOUNT)) {
        tooltip.add(FluidText.amount(fluid.getAmount()).withStyle(ChatFormatting.GRAY));
      }
      tooltip.add(FluidText.capacity(capacity).withStyle(ChatFormatting.GRAY));
      tooltip.add(FluidText.modName(fluid));
    }
    int x = minecraft.getWindow().getGuiScaledWidth() / 2;
    int y = minecraft.getWindow().getGuiScaledHeight() / 2;
    graphics.renderTooltip(minecraft.font, tooltip, Optional.empty(), x, y);
  }
}
