package org.envel.immersiveportalspaperized.bukkit.portal.spawning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.ChunkPosition;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.SpiralChunkAreaIterator;
import org.envel.immersiveportalspaperized.bukkit.config.PortalSpawnConfig;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * AsyncPortalChecker.
 */
public class AsyncPortalChecker implements Runnable {
   private static final double CHUNK_SKIP_DISTANCE = 45.0;
   private static final double PORTAL_SEARCH_RADIUS = 128.0;
   private final Logger logger;
   private final PortalSpawningContext context;
   private final PortalSpawnConfig config;
   private final Iterator<ChunkPosition> iterator;
   private final IChunkChecker chunkChecker;
   private final Consumer<PortalSpawnPosition> onFinish;
   private SchedulerUtil.PortalTask repeatingTask;
   private PortalSpawnPosition currentClosest;
   private double closestDistance = Double.POSITIVE_INFINITY;
   private int updateCount;

   public AsyncPortalChecker(
      PortalSpawningContext context, IChunkChecker chunkChecker, Consumer<PortalSpawnPosition> onFinish, JavaPlugin pl, Logger logger, PortalSpawnConfig config
   ) {
      this.logger = logger;
      this.context = context;
      this.config = config;
      this.chunkChecker = chunkChecker;
      this.onFinish = onFinish;
      Location spawnPos = context.getPreferredLocation();
      this.iterator = new SpiralChunkAreaIterator(
         spawnPos.clone().subtract(PORTAL_SEARCH_RADIUS, 0.0, PORTAL_SEARCH_RADIUS), spawnPos.clone().add(PORTAL_SEARCH_RADIUS, 0.0, PORTAL_SEARCH_RADIUS)
      );
      if (SchedulerUtil.isFolia()) {
         this.repeatingTask = null;
         List<ChunkPosition> chunksToCheck = new ArrayList<>();

         while (this.iterator.hasNext()) {
            chunksToCheck.add(this.iterator.next());
         }

         int totalChunks = chunksToCheck.size();
         if (totalChunks == 0) {
            onFinish.accept(null);
            return;
         }

         AtomicInteger completedCount = new AtomicInteger(0);

         for (ChunkPosition chunk : chunksToCheck) {
            SchedulerUtil.runAtLocation(spawnPos.getWorld(), chunk.x << 4, chunk.z << 4, () -> {
               try {
                  double closestTheoreticalDistanceInChunk = chunk.getCenterPos().distance(context.getPreferredLocation()) - CHUNK_SKIP_DISTANCE;
                  boolean shouldCheck;
                  synchronized (this) {
                     shouldCheck = closestTheoreticalDistanceInChunk < this.closestDistance;
                  }

                  if (shouldCheck) {
                     PortalSpawnPosition result = chunkChecker.findClosestInChunk(chunk, context);
                     if (result != null) {
                        double distance = result.getPosition().distance(context.getPreferredLocation());
                        synchronized (this) {
                           if (distance < this.closestDistance) {
                              this.closestDistance = distance;
                              this.currentClosest = result;
                           }
                        }
                     }
                  }
               } finally {
                  if (completedCount.incrementAndGet() == totalChunks) {
                     SchedulerUtil.runTask(this::onFinish);
                  }
               }
            });
         }
      } else {
         this.repeatingTask = SchedulerUtil.runTaskTimer(this, 1L, 1L);
      }
   }

   @Override
   public void run() {
      this.updateCount++;
      long startNs = System.nanoTime();
      long allowedNs = (long)(this.config.getAllowedSpawnTimePerTick() * 1000000.0);

      while (System.nanoTime() - startNs < allowedNs) {
         if (!this.iterator.hasNext()) {
            this.onFinish();
            return;
         }

         this.checkChunk(this.iterator.next());
      }
   }

   private void onFinish() {
      this.logger.fine("Finished delayed portal check within %d ticks", this.updateCount);
      if (this.repeatingTask != null) {
         this.repeatingTask.cancel();
      }

      this.onFinish.accept(this.currentClosest);
   }

   private void checkChunk(ChunkPosition chunk) {
      double closestTheoreticalDistanceInChunk = chunk.getCenterPos().distance(this.context.getPreferredLocation()) - CHUNK_SKIP_DISTANCE;
      if (!(closestTheoreticalDistanceInChunk > this.closestDistance)) {
         PortalSpawnPosition result = this.chunkChecker.findClosestInChunk(chunk, this.context);
         if (result != null) {
            double distance = result.getPosition().distance(this.context.getPreferredLocation());
            if (distance < this.closestDistance) {
               this.closestDistance = distance;
               this.currentClosest = result;
            }
         }
      }
   }
}


