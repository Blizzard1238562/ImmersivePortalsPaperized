package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public abstract class EntityTrackingManager {
   private final IEntityTracker.Factory entityTrackerFactory;
   protected final Map<IPortal, Map<Entity, IEntityTracker>> trackersByPortal = new ConcurrentHashMap<>();
   protected final Logger logger;

   @Inject
   public EntityTrackingManager(Logger logger, IEntityTracker.Factory entityTrackerFactory) {
      this.logger = logger;
      this.entityTrackerFactory = entityTrackerFactory;
   }

   public void setTracking(Entity entity, IPortal portal, Player player) {
      Map<Entity, IEntityTracker> portalMap = this.trackersByPortal.computeIfAbsent(portal, k -> new ConcurrentHashMap<>());
      IEntityTracker tracker = portalMap.computeIfAbsent(entity, k -> {
         IEntityTracker newTracker = this.entityTrackerFactory.create(entity, portal);
         this.newTrackerAdded(newTracker);
         return newTracker;
      });
      tracker.addTracking(player);
   }

   protected void newTrackerAdded(IEntityTracker tracker) {
   }

   protected void trackerHasNoPlayers(IEntityTracker tracker) {
   }

   public void setNoLongerTracking(Entity entity, IPortal portal, Player player, boolean sendPackets) {
      Map<Entity, IEntityTracker> portalMap = this.trackersByPortal.get(portal);
      IEntityTracker tracker = portalMap == null ? null : portalMap.get(entity);
      if (tracker == null) {
         this.logger.fine("Attempted to remove entity tracker that didn't exist. This should never happen!");
      } else {
         tracker.removeTracking(player, sendPackets);
         if (tracker.getTrackingPlayerCount() == 0) {
            this.trackerHasNoPlayers(tracker);
            portalMap.remove(entity);
            if (portalMap.isEmpty()) {
               this.trackersByPortal.remove(portal);
            }
         }
      }
   }

   public void update() {
      this.trackersByPortal.values().forEach(map -> map.values().forEach(tracker -> {
         if (SchedulerUtil.isFolia()) {
            SchedulerUtil.runForEntity(tracker.getEntityInfo().getEntity(), tracker::update);
         } else {
            tracker.update();
         }
      }));
   }

   @Nullable
   public IEntityTracker getTracker(IPortal portal, Entity entity) {
      Map<Entity, IEntityTracker> portalTrackers = this.trackersByPortal.get(portal);
      return portalTrackers == null ? null : portalTrackers.get(entity);
   }
}
