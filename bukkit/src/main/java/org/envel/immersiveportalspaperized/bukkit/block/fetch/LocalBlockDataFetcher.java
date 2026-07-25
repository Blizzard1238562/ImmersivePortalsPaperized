package org.envel.immersiveportalspaperized.bukkit.block.fetch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import org.jetbrains.annotations.NotNull;

public class LocalBlockDataFetcher implements IBlockDataFetcher {
   private final IPortal portal;
   private final World destinationWorld;
   private final RenderConfig renderConfig;
   private final Map<IntVector, BlockData> cachedStates = new ConcurrentHashMap<>();
   private final AtomicBoolean isFetching = new AtomicBoolean(false);
   private volatile boolean isReady = false;

   public LocalBlockDataFetcher(IPortal portal, RenderConfig renderConfig) {
      this.portal = portal;
      this.destinationWorld = portal.getDestPos().getWorld();
      this.renderConfig = renderConfig;
   }

   @Override
   public void update() {
      if (SchedulerUtil.isFolia()) {
         if (this.destinationWorld != null && this.isFetching.compareAndSet(false, true)) {
            IntVector destIntPos = this.portal.getDestPos().getIntVector();
            int destX = destIntPos.getX();
            int destZ = destIntPos.getZ();
            SchedulerUtil.runAtLocation(this.destinationWorld, destX, destZ, () -> {
               try {
                  int maxXZ = (int)this.renderConfig.getMaxXZ();
                  int maxY = (int)this.renderConfig.getMaxY();
                  IntVector center = new IntVector(this.portal.getDestPos().getVector());

                  for (int x = -maxXZ; x <= maxXZ; x++) {
                     for (int z = -maxXZ; z <= maxXZ; z++) {
                        for (int y = -maxY; y <= maxY; y++) {
                           IntVector relPos = new IntVector(x, y, z);
                           IntVector absPos = center.add(relPos);

                           try {
                              BlockData data = this.destinationWorld.getBlockData(absPos.getX(), absPos.getY(), absPos.getZ());
                              this.cachedStates.put(absPos, data);
                           } catch (Exception var13) {
                           }
                        }
                     }
                  }

                  this.isReady = true;
               } finally {
                  this.isFetching.set(false);
               }
            });
         }
      }
   }

   @Override
   public boolean isReady() {
      return SchedulerUtil.isFolia() ? this.isReady : true;
   }

   @NotNull
   @Override
   public BlockData getData(@NotNull IntVector position) {
      if (SchedulerUtil.isFolia()) {
         BlockData data = this.cachedStates.get(position);
         return data == null ? Bukkit.createBlockData(Material.AIR) : data;
      } else {
         return position.getBlock(this.destinationWorld).getBlockData();
      }
   }
}
