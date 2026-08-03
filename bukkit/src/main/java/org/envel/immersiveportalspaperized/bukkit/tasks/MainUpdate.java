package org.envel.immersiveportalspaperized.bukkit.tasks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.block.external.IExternalBlockWatcherManager;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityTrackingManager;
import org.envel.immersiveportalspaperized.bukkit.net.ClientRequestHandler;
import org.envel.immersiveportalspaperized.bukkit.player.PlayerDataManager;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalActivityManager;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * MainUpdate.
 */
@Singleton
public class MainUpdate implements Runnable {
   private static final String ISSUES_URL = "https://discord.gg/wTVNTJsBUr";
   private final JavaPlugin pl;
   private final PlayerDataManager playerDataManager;
   private final IPortalActivityManager activityManager;
   private final EntityTrackingManager entityTrackingManager;
   private final ClientRequestHandler requestHandler;
   private final IExternalBlockWatcherManager blockWatcherManager;
   private final MiscConfig miscConfig;
   private final Logger logger;
   private static final int TPS_CACHE_TICKS = 5;
   private int tpsCacheAge = 5;
   private boolean tpsTooLow = false;
   private double minTpsThreshold;
   private boolean minTpsEnabled;
   private SchedulerUtil.PortalTask updateTask;

   @Inject
   public MainUpdate(
      JavaPlugin pl,
      PlayerDataManager playerDataManager,
      IPortalActivityManager activityManager,
      EntityTrackingManager entityTrackingManager,
      ClientRequestHandler requestHandler,
      IExternalBlockWatcherManager blockWatcherManager,
      MiscConfig miscConfig,
      Logger logger
   ) {
      this.pl = pl;
      this.playerDataManager = playerDataManager;
      this.activityManager = activityManager;
      this.entityTrackingManager = entityTrackingManager;
      this.requestHandler = requestHandler;
      this.blockWatcherManager = blockWatcherManager;
      this.miscConfig = miscConfig;
      this.logger = logger;
   }

   public void start() {
      if (this.updateTask != null) {
         this.updateTask.cancel();
      }

      this.minTpsThreshold = this.miscConfig.getMinTpsForRendering();
      this.minTpsEnabled = this.minTpsThreshold > 0.0;
      this.tpsCacheAge = TPS_CACHE_TICKS;
      this.updateTask = SchedulerUtil.runTaskTimer(this, 0L, 1L);
   }

   public void stop() {
      if (this.updateTask != null) {
         this.updateTask.cancel();
         this.updateTask = null;
      }
   }

   @Override
   public void run() {
      try {
         boolean skipRendering = this.isTpsTooLowCached();
         if (SchedulerUtil.isFolia()) {
            this.playerDataManager
               .getPlayers()
               .forEach(playerData -> SchedulerUtil.runForEntity(playerData.getPlayer(), () -> playerData.onUpdate(skipRendering)));
         } else {
            this.playerDataManager.getPlayers().forEach(pd -> pd.onUpdate(skipRendering));
         }

         this.entityTrackingManager.update();
         this.activityManager.postUpdate();
         this.requestHandler.handlePendingRequests();
         this.blockWatcherManager.update();
      } catch (RuntimeException e) {
         this.logger.severe("A critical error occurred during main update.");
         this.logger.severe("Please create an issue at %s to get this fixed.", ISSUES_URL);
         this.logger.severe("Error: %s", e.getMessage());
      }
   }

   private boolean isTpsTooLowCached() {
      if (!this.minTpsEnabled) {
         return false;
      } else {
         if (++this.tpsCacheAge >= TPS_CACHE_TICKS) {
            this.tpsCacheAge = 0;
            double[] tps = Bukkit.getTPS();
            this.tpsTooLow = tps.length > 0 && tps[0] < this.minTpsThreshold;
         }

         return this.tpsTooLow;
      }
   }
}


