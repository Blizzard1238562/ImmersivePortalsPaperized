package org.envel.immersiveportalspaperized.bukkit.block.external;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetBlockDataChangesRequest;
import org.envel.immersiveportalspaperized.bukkit.nms.BlockDataUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.jetbrains.annotations.NotNull;

/**
 * BlockChangeWatcher.
 */
public class BlockChangeWatcher implements IBlockChangeWatcher {
   private final IntVector center;
   private final Matrix rotationMatrix;
   private World world;
   private final int xAndZRadius;
   private final int yRadius;
   private final Map<IntVector, BlockData> previousData = new HashMap<>();

   @Inject
   public BlockChangeWatcher(@Assisted GetBlockDataChangesRequest request) {
      this.center = request.getPosition();
      this.rotationMatrix = request.getRotateOriginToDest();
      this.xAndZRadius = request.getXAndZRadius();
      this.yRadius = request.getYRadius();
      this.world = Bukkit.getWorld(request.getWorldId());
      if (this.world == null) {
         this.world = Bukkit.getWorld(request.getWorldName());
      }
   }

   @NotNull
   @Override
   public Map<IntVector, Integer> checkForChanges() {
      Map<IntVector, Integer> result = new HashMap<>();

      for (int x = -this.xAndZRadius; x <= this.xAndZRadius; x++) {
         for (int z = -this.xAndZRadius; z <= this.xAndZRadius; z++) {
            for (int y = -this.yRadius; y <= this.yRadius; y++) {
               IntVector blockPos = this.rotationMatrix.transform(new IntVector(x, y, z)).add(this.center);
               BlockData data = blockPos.getBlock(this.world).getBlockData();
               BlockData oldData = this.previousData.get(blockPos);
               if (!data.equals(oldData)) {
                  result.put(blockPos, BlockDataUtil.getCombinedId(data));
                  this.previousData.put(blockPos, data);
               }
            }
         }
      }

      return result;
   }
}


