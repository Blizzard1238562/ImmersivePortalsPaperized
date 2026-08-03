package org.envel.immersiveportalspaperized.bukkit.player.view.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityInfo;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityTrackingManager;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.IEntityPacketManipulator;
import org.envel.immersiveportalspaperized.bukkit.math.PlaneIntersectionChecker;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * PlayerEntityView.
 */
public class PlayerEntityView implements IPlayerEntityView {
   private final Logger logger;
   private final IPortal portal;
   private final Player player;
   private final IEntityPacketManipulator packetManipulator;
   private final EntityTrackingManager trackingManager;
   private final Map<Entity, EntityInfo> hiddenEntities = new HashMap<>();
   private final Set<Entity> replicatedEntities = new HashSet<>();

   @Inject
   public PlayerEntityView(
      @Assisted IPortal portal, @Assisted Player player, IEntityPacketManipulator packetManipulator, Logger logger, EntityTrackingManager trackingManager
   ) {
      this.portal = portal;
      this.player = player;
      this.packetManipulator = packetManipulator;
      this.logger = logger;
      this.trackingManager = trackingManager;
   }

   @Override
   public void update() {
      this.updateHiddenEntities();
      if (!this.portal.isCrossServer()) {
         this.updateReplicatedEntities();
      }
   }

   private void updateHiddenEntities() {
      PlaneIntersectionChecker intersectionChecker = this.portal.getTransformations().createIntersectionChecker(this.player.getEyeLocation().toVector());
      Set<Entity> nowHidden = new HashSet<>();

      for (Entity entity : this.portal.getEntityList().getOriginEntities()) {
         if (entity != this.player) {
            boolean shouldBeHidden = intersectionChecker.checkIfIntersects(entity.getLocation().toVector());
            if (shouldBeHidden) {
               nowHidden.add(entity);
               if (!this.hiddenEntities.containsKey(entity)) {
                  this.hide(entity);
               }
            }
         }
      }

      this.hiddenEntities.entrySet().removeIf(entry -> {
         boolean isHidden = nowHidden.contains(entry.getKey());
         if (!isHidden && entry.getKey().isValid()) {
            this.packetManipulator.showEntity(entry.getValue(), this.player);
         }

         return !isHidden;
      });
   }

   private void updateReplicatedEntities() {
      PlaneIntersectionChecker intersectionChecker = this.portal.getTransformations().createIntersectionChecker(this.player.getEyeLocation().toVector());
      Set<Entity> nowReplicated = new HashSet<>();

      for (Entity entity : this.portal.getEntityList().getDestinationEntities()) {
         if (!this.isVanished(entity)) {
            Location originPos = this.portal.getTransformations().moveToOrigin(entity.getLocation());
            boolean shouldBeReplicated = intersectionChecker.checkIfIntersects(originPos.toVector());
            if (shouldBeReplicated) {
               nowReplicated.add(entity);
               if (!this.replicatedEntities.contains(entity)) {
                  this.replicatedEntities.add(entity);
                  this.trackingManager.setTracking(entity, this.portal, this.player);
               }
            }
         }
      }

      this.replicatedEntities.removeIf(entityx -> {
         boolean isReplicated = nowReplicated.contains(entityx);
         if (!isReplicated) {
            this.trackingManager.setNoLongerTracking(entityx, this.portal, this.player, true);
         }

         return !isReplicated;
      });
   }

   private boolean isVanished(Entity entity) {
      if (!(entity instanceof Player)) {
         return false;
      } else {
         for (MetadataValue value : entity.getMetadata("vanished")) {
            if (value.asBoolean()) {
               return true;
            }
         }

         return false;
      }
   }

   private void hide(Entity entity) {
      EntityInfo entityInfo = new EntityInfo(entity);
      this.packetManipulator.hideEntity(entityInfo, this.player);
      this.hiddenEntities.put(entity, entityInfo);
   }

   @Override
   public void onDeactivate(boolean shouldResetEntities) {
      if (shouldResetEntities) {
         this.hiddenEntities.forEach((entity, entityInfo) -> this.packetManipulator.showEntity(entityInfo, this.player));
      }

      this.replicatedEntities.forEach(entity -> this.trackingManager.setNoLongerTracking(entity, this.portal, this.player, shouldResetEntities));
   }
}


