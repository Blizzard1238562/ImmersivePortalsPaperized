package org.envel.immersiveportalspaperized.bukkit.block.bukkit;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.block.IViewableBlockInfo;

/**
 * BukkitBlockInfo.
 */
@Getter
public class BukkitBlockInfo implements IViewableBlockInfo {
   private final IntVector originPos;
   private BlockData baseOriginData;
   @Setter
   private BlockData baseDestData;
   private WrappedBlockState originData;
   private WrappedBlockState renderedDestData;

   public BukkitBlockInfo(IntVector originPos, BlockData originData, BlockData destData) {
      this.originPos = originPos;
      this.baseOriginData = originData;
      this.baseDestData = destData;
      this.originData = SpigotConversionUtil.fromBukkitBlockData(originData);
   }

   public void setOriginData(BlockData originData) {
      this.baseOriginData = originData;
      this.originData = SpigotConversionUtil.fromBukkitBlockData(originData);
   }

   public void setRenderedDestData(WrappedBlockState destData) {
      this.renderedDestData = destData;
   }
}


