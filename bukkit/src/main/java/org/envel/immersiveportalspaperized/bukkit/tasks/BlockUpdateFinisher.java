package org.envel.immersiveportalspaperized.bukkit.tasks;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.envel.immersiveportalspaperized.bukkit.player.view.block.PlayerBlockView;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public abstract class BlockUpdateFinisher {
   private final BlockingQueue<BlockUpdateFinisher.BlockViewUpdateInfo> updateQueue = new LinkedBlockingQueue<>();
   protected final Logger logger;
   private volatile boolean hasStopped = false;

   protected BlockUpdateFinisher(Logger logger) {
      this.logger = logger;
   }

   private void processUpdate(BlockUpdateFinisher.BlockViewUpdateInfo next) {
      if (next.type == BlockUpdateFinisher.BlockViewUpdateType.RESET) {
         this.logger.fine("Running scheduled reset");
         next.blockView.finishReset();
      } else {
         next.blockView.finishUpdate(next.type == BlockUpdateFinisher.BlockViewUpdateType.REFRESH);
      }
   }

   protected void processUpdatesContinually() {
      try {
         while (!this.hasStopped) {
            BlockUpdateFinisher.BlockViewUpdateInfo next = this.updateQueue.take();
            this.processUpdate(next);
         }
      } catch (InterruptedException var2) {
      }
   }

   protected void finishPendingUpdates() {
      while (true) {
         BlockUpdateFinisher.BlockViewUpdateInfo next = this.updateQueue.poll();
         if (next == null) {
            return;
         }

         this.processUpdate(next);
      }
   }

   public void start() {
      this.hasStopped = false;
   }

   public void stop() {
      this.hasStopped = true;
      this.updateQueue.clear();
   }

   public void scheduleUpdate(PlayerBlockView blockView, boolean refresh) {
      BlockUpdateFinisher.BlockViewUpdateInfo updateInfo = new BlockUpdateFinisher.BlockViewUpdateInfo(
         blockView, refresh ? BlockUpdateFinisher.BlockViewUpdateType.REFRESH : BlockUpdateFinisher.BlockViewUpdateType.REGULAR
      );
      if (this.updateQueue.contains(updateInfo)) {
         this.logger.fine("Block update was scheduled when previous update had not finished. Server is running behind!");
      } else {
         try {
            this.updateQueue.put(updateInfo);
         } catch (InterruptedException e) {
            this.logger.warning("Interrupted while scheduling block update: %s", e.getMessage());
            Thread.currentThread().interrupt();
         }
      }
   }

   public void scheduleReset(PlayerBlockView blockView) {
      try {
         BlockUpdateFinisher.BlockViewUpdateInfo updateInfo = new BlockUpdateFinisher.BlockViewUpdateInfo(
            blockView, BlockUpdateFinisher.BlockViewUpdateType.RESET
         );
         this.updateQueue.remove(updateInfo);
         this.updateQueue.put(updateInfo);
      } catch (InterruptedException e) {
         this.logger.warning("Interrupted while scheduling block reset: %s", e.getMessage());
         Thread.currentThread().interrupt();
      }
   }

   private static class BlockViewUpdateInfo {
      PlayerBlockView blockView;
      BlockUpdateFinisher.BlockViewUpdateType type;

      public BlockViewUpdateInfo(PlayerBlockView blockView, BlockUpdateFinisher.BlockViewUpdateType type) {
         this.blockView = blockView;
         this.type = type;
      }

      @Override
      public boolean equals(Object other) {
         return !(other instanceof BlockUpdateFinisher.BlockViewUpdateInfo)
            ? false
            : this.blockView == ((BlockUpdateFinisher.BlockViewUpdateInfo)other).blockView;
      }
   }

   private static enum BlockViewUpdateType {
      REGULAR,
      REFRESH,
      RESET;
   }
}
