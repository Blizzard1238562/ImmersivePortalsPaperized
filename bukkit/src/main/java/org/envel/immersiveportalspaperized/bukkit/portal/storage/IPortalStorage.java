package org.envel.immersiveportalspaperized.bukkit.portal.storage;

import java.io.IOException;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public abstract class IPortalStorage implements Runnable {
   protected Logger logger;
   private final JavaPlugin pl;
   private final MiscConfig miscConfig;
   private SchedulerUtil.PortalTask saveTask;

   public IPortalStorage(Logger logger, JavaPlugin pl, MiscConfig miscConfig) {
      this.logger = logger;
      this.pl = pl;
      this.miscConfig = miscConfig;
   }

   public abstract void loadPortals() throws IOException;

   public abstract void savePortals() throws IOException;

   public void start() {
      this.stop();
      int saveInterval = this.miscConfig.getPortalSaveInterval();
      if (saveInterval > 0) {
         this.logger.fine("Starting autosave task");
         this.saveTask = SchedulerUtil.runTaskTimer(this, saveInterval, saveInterval);
      } else {
         this.logger.fine("Autosave is disabled");
      }
   }

   public void stop() {
      if (this.saveTask != null) {
         this.logger.fine("Stopping autosave task");
         this.saveTask.cancel();
         this.saveTask = null;
      }
   }

   @Override
   public void run() {
      try {
         this.logger.fine("Autosaving portals!");
         this.savePortals();
      } catch (IOException var2) {
         this.logger.warning("Error occurred while saving the portals to portals.yml. Check your file permissions!");
         var2.printStackTrace();
      }
   }
}
