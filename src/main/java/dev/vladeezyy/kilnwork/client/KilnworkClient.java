package dev.vladeezyy.kilnwork.client;

import dev.vladeezyy.kilnwork.Kilnwork;
import dev.vladeezyy.kilnwork.client.render.KilnworkFluidModels;
import dev.vladeezyy.kilnwork.fluid.FluidText;
import dev.vladeezyy.kilnwork.tag.KilnworkTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Client side of Kilnwork: data loaders and the gauge tooltip */
@EventBusSubscriber(modid = Kilnwork.MOD_ID, value = Dist.CLIENT)
public final class KilnworkClient {
  private KilnworkClient() {}

  @SubscribeEvent
  static void registerReloadListeners(AddClientReloadListenersEvent event) {
    event.addListener(Kilnwork.id("block_fluids"), KilnworkFluidModels.BLOCK_FLUIDS);
    event.addListener(Kilnwork.id("faucet_fluids"), KilnworkFluidModels.FAUCET_FLUIDS);
    event.addListener(Kilnwork.id("channel_fluids"), KilnworkFluidModels.CHANNEL_FLUIDS);
    NeoForge.EVENT_BUS.addListener(KilnworkClient::renderGaugeTooltip);
  }

  /** Current level on the client, null if not in a world */
  @Nullable
  public static Level getLevel() {
    return Minecraft.getInstance().level;
  }

  /** Shows the fluid of a gauge or tank tagged in {@link KilnworkTags.Blocks#GAUGES} next to the crosshair */
  private static void renderGaugeTooltip(RenderGuiEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.gui.screen() != null && minecraft.gui.screen().getClass() != ChatScreen.class) {
      return;
    }
    if (minecraft.level == null || minecraft.gui.hud.isHidden() || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
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
    ResourceHandler<FluidResource> handler = minecraft.level.getCapability(Capabilities.Fluid.BLOCK, fluidPos, side);
    if (handler == null || handler.size() <= 0) {
      return;
    }
    FluidStack fluid = FluidUtil.getStack(handler, 0);
    int capacity = handler.getCapacityAsInt(0, FluidResource.EMPTY);
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
    event.getGuiGraphics().setTooltipForNextFrame(minecraft.font, tooltip, Optional.empty(), ItemStack.EMPTY, x, y);
  }
}
