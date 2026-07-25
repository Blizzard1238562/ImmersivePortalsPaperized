package org.envel.immersiveportalspaperized.bukkit.block.bukkit;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.block.IViewableBlockInfo;

@Getter
public class BukkitBlockInfo implements IViewableBlockInfo {
   private final IntVector originPos;
   private BlockData baseOriginData;
   @Setter
   private BlockData baseDestData;
   private WrappedBlockData originData;
   private WrappedBlockData renderedDestData;

   public BukkitBlockInfo(IntVector originPos, BlockData originData, BlockData destData) {
      this.originPos = originPos;
      this.baseOriginData = originData;
      this.baseDestData = destData;
      this.originData = WrappedBlockData.createData(originData);
   }

   public void setOriginData(BlockData originData) {
      this.baseOriginData = originData;
      this.originData = WrappedBlockData.createData(originData);
   }

   public void setRenderedDestData(WrappedBlockData destData) {
      this.renderedDestData = destData;
   }
}
