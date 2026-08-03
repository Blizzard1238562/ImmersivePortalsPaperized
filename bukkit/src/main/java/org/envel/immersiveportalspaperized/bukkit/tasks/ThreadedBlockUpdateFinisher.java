package org.envel.immersiveportalspaperized.bukkit.tasks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * ThreadedBlockUpdateFinisher.
 */
@Singleton
public class ThreadedBlockUpdateFinisher extends BlockUpdateFinisher implements Runnable {
   private Thread thread;

   @Inject
   public ThreadedBlockUpdateFinisher(Logger logger) {
      super(logger);
   }

   @Override
   public void start() {
      super.start();
      this.thread = new Thread(this, "ImmersivePortalsPaperized View Update Thread");
      this.thread.start();
   }

   @Override
   public void stop() {
      if (this.thread != null) {
         this.thread.interrupt();
      }

      super.stop();
   }

   @Override
   public void run() {
      this.logger.fine("Hello from block view update thread!");
      super.processUpdatesContinually();
      this.logger.fine("Goodbye from block view update thread!");
   }
}


