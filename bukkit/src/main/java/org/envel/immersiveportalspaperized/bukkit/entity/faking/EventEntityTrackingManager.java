package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.envel.immersiveportalspaperized.bukkit.events.IEventRegistrar;
import org.envel.immersiveportalspaperized.bukkit.nms.AnimationType;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * EventEntityTrackingManager.
 */
@Singleton
public class EventEntityTrackingManager extends EntityTrackingManager implements Listener {
   private final Map<Entity, List<IEntityTracker>> trackersByEntity = new ConcurrentHashMap<>();
   private final Map<Entity, EquipmentSlot> lastHandUsed = new ConcurrentHashMap<>();

   @Inject
   public EventEntityTrackingManager(Logger logger, IEventRegistrar eventRegistrar, IEntityTracker.Factory entityTrackerFactory) {
      super(logger, entityTrackerFactory);
      eventRegistrar.register(this);
   }

   @Override
   protected void newTrackerAdded(IEntityTracker tracker) {
      this.trackersByEntity.computeIfAbsent(tracker.getEntityInfo().getEntity(), entity -> new CopyOnWriteArrayList<>()).add(tracker);
   }

   @Override
   protected void trackerHasNoPlayers(IEntityTracker tracker) {
      List<IEntityTracker> trackersForEntity = this.trackersByEntity.get(tracker.getEntityInfo().getEntity());
      if (trackersForEntity != null) {
         trackersForEntity.remove(tracker);
         if (trackersForEntity.isEmpty()) {
            this.trackersByEntity.remove(tracker.getEntityInfo().getEntity());
         }
      }
   }

   private void forEachTracker(Entity entity, Consumer<IEntityTracker> action) {
      Collection<IEntityTracker> trackers = this.trackersByEntity.get(entity);
      if (trackers != null) {
         trackers.forEach(action);
      }
   }

   @EventHandler
   public void onEntityDamage(EntityDamageEvent event) {
      this.forEachTracker(event.getEntity(), tracker -> tracker.onAnimation(AnimationType.DAMAGE));
   }

   @EventHandler
   public void onPlayerAnimation(PlayerAnimationEvent event) {
      if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
         EquipmentSlot hand = this.lastHandUsed.get(event.getPlayer());
         if (hand != null) {
            AnimationType type = hand == EquipmentSlot.HAND ? AnimationType.MAIN_HAND : AnimationType.OFF_HAND;
            this.forEachTracker(event.getPlayer(), tracker -> tracker.onAnimation(type));
         }
      }
   }

   @EventHandler
   public void onEntityPickupItem(EntityPickupItemEvent event) {
      Entity entity = event.getEntity();
      this.forEachTracker(entity, tracker -> {
         Map<Entity, IEntityTracker> portalTrackers = this.trackersByPortal.get(tracker.getPortal());
         IEntityTracker pickedUp = portalTrackers.get(event.getItem());
         if (pickedUp != null) {
            this.logger.fine("Sending pickup packet");
            tracker.onPickup(pickedUp.getEntityInfo());
         } else {
            this.logger.fine("Not sending pickup packet - the item isn't viewable");
         }
      });
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      EquipmentSlot hand = event.getHand();
      if (hand != null) {
         this.lastHandUsed.put(event.getPlayer(), hand);
      }
   }

   @Nullable
   @Override
   public IEntityTracker getTracker(IPortal portal, Entity entity) {
      Map<Entity, IEntityTracker> portalTrackers = this.trackersByPortal.get(portal);
      return portalTrackers == null ? null : portalTrackers.get(entity);
   }
}


