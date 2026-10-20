package dev.vladeezyy.kilnwork.retexture;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Item of a {@link RetexturedBlock}: names the texture block in the tooltip (blocks have no tooltip of their own since 1.21.5) */
public class RetexturedBlockItem extends BlockItem {
  public RetexturedBlockItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, display, tooltip, flag);
    RetexturedHelper.addTooltip(stack, tooltip, flag);
  }
}
