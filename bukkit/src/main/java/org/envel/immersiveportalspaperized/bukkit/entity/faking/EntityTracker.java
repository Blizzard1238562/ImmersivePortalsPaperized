package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.nms.AnimationType;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.jetbrains.annotations.NotNull;

/**
 * EntityTracker.
 */
public class EntityTracker implements IEntityTracker {
   private static final int FAKE_PLAYER_TAB_LIST_REMOVE_DELAY = 20;
   private final Entity entity;
   @Getter
   private final EntityInfo entityInfo;
   @Getter
   private final IPortal portal;
   private final IEntityPacketManipulator packetManipulator;
   private final EntityTrackingManager entityTrackingManager;
   private final JavaPlugin pl;
   private final Set<Player> trackingPlayers = ConcurrentHashMap.newKeySet();
   private final EntityEquipmentWatcher equipmentWatcher;
   private Vector lastPosition;
   private Vector lastDirection;
   private Vector lastVelocity;
   private float lastHeadRotation;
   private List<Entity> lastMounts;
   private final int metadataUpdateInterval;
   private int ticksSinceCreated = 0;

   @Inject
   public EntityTracker(
      @Assisted Entity entity,
      @Assisted IPortal portal,
      IEntityPacketManipulator packetManipulator,
      EntityTrackingManager entityTrackingManager,
      RenderConfig renderConfig,
      JavaPlugin pl
   ) {
      this.equipmentWatcher = entity instanceof LivingEntity ? new EntityEquipmentWatcher((LivingEntity)entity) : null;
      this.entity = entity;
      this.entityTrackingManager = entityTrackingManager;
      this.portal = portal;
      this.entityInfo = new EntityInfo(portal.getTransformations(), entity);
      this.packetManipulator = packetManipulator;
      this.metadataUpdateInterval = renderConfig.getEntityMetadataUpdateInterval();
      this.pl = pl;
   }

   @Override
   public void update() {
      this.sendMovementUpdates();
      if (this.equipmentWatcher != null) {
         Map<EquipmentSlot, ItemStack> equipmentChanges = this.equipmentWatcher.checkForChanges();
         if (!equipmentChanges.isEmpty()) {
            this.packetManipulator.sendEntityEquipment(this.entityInfo, equipmentChanges, this.trackingPlayers);
         }
      }

      List<Entity> newMounts = this.entity.getPassengers();
      if (!newMounts.equals(this.lastMounts)) {
         this.lastMounts = newMounts;
         List<EntityInfo> visibleMounts = new ArrayList<>();

         for (Entity entity : newMounts) {
            IEntityTracker tracker = this.entityTrackingManager.getTracker(this.portal, entity);
            if (tracker != null) {
               visibleMounts.add(tracker.getEntityInfo());
            }
         }

         this.packetManipulator.sendMount(this.entityInfo, visibleMounts, this.trackingPlayers);
      }

      if (this.ticksSinceCreated % this.metadataUpdateInterval == 0) {
         this.packetManipulator.sendMetadata(this.entityInfo, this.trackingPlayers);
      }

      this.packetManipulator.sendEntityHeadRotation(this.entityInfo, this.trackingPlayers);
      Vector velocity = this.entity.getVelocity();
      if (this.lastVelocity != null && !velocity.equals(this.lastVelocity)) {
         this.packetManipulator.sendEntityVelocity(this.entityInfo, velocity, this.trackingPlayers);
      }

      this.lastVelocity = velocity;

      this.ticksSinceCreated++;
   }

   @Override
   public void onAnimation(@NotNull AnimationType animationType) {
      this.packetManipulator.sendEntityAnimation(this.entityInfo, this.trackingPlayers, animationType);
   }

   @Override
   public void onPickup(@NotNull EntityInfo pickedUp) {
      this.packetManipulator.sendEntityPickupItem(this.entityInfo, pickedUp, this.trackingPlayers);
   }

   private void sendMovementUpdates() {
      Vector currentPosition = this.entity.getLocation().toVector();
      Vector currentDirection = this.entity.getLocation().getDirection();
      boolean positionChanged = this.lastPosition != null && !currentPosition.equals(this.lastPosition);
      boolean rotationChanged = this.lastDirection != null && !currentDirection.equals(this.lastDirection);
      Vector posOffset = this.lastPosition == null ? new Vector() : currentPosition.clone().subtract(this.lastPosition);
      this.lastPosition = currentPosition;
      this.lastDirection = currentDirection;
      // NOTE: must check the absolute distance, not just the upper bound - a relative-move
      // packet can only encode an offset of roughly +-8 blocks per axis. The previous check
      // (offset < 8.0) let large *negative* offsets slip through as "safe", which would have
      // sent an offset the client can't represent correctly instead of falling back to a
      // teleport, causing the fake entity to glitch when moving fast in the negative direction.
      boolean canUseRelativeMove = Math.abs(posOffset.getX()) < 8.0 && Math.abs(posOffset.getY()) < 8.0 && Math.abs(posOffset.getZ()) < 8.0;
      if (positionChanged && !canUseRelativeMove) {
         this.packetManipulator.sendEntityTeleport(this.entityInfo, this.trackingPlayers);
      } else if (positionChanged && rotationChanged) {
         this.packetManipulator.sendEntityMoveLook(this.entityInfo, posOffset, this.trackingPlayers);
      } else if (positionChanged) {
         this.packetManipulator.sendEntityMove(this.entityInfo, posOffset, this.trackingPlayers);
      } else if (rotationChanged) {
         this.packetManipulator.sendEntityLook(this.entityInfo, this.trackingPlayers);
      }

      float headRotation = this.entity.getLocation().getYaw();
      if (this.lastHeadRotation != headRotation) {
         this.lastHeadRotation = headRotation;
         this.packetManipulator.sendEntityHeadRotation(this.entityInfo, this.trackingPlayers);
      }
   }

   @Override
   public void addTracking(@NotNull Player player) {
      if (this.trackingPlayers.contains(player)) {
         throw new IllegalArgumentException("Player is already tracking this entity");
      } else {
         this.trackingPlayers.add(player);
         if (SchedulerUtil.isFolia()) {
            SchedulerUtil.runForEntity(
               this.entity,
               () -> {
                  if (this.trackingPlayers.contains(player)) {
                     boolean sendingPlayerProfilex = !this.entityInfo.getEntityUniqueId().equals(this.entityInfo.getEntity().getUniqueId())
                        && this.entityInfo.getEntity() instanceof Player;
                     if (sendingPlayerProfilex) {
                        this.packetManipulator.sendAddPlayerProfile(this.entityInfo, Collections.singleton(player));
                     }

                     this.packetManipulator.showEntity(this.entityInfo, player);
                     if (sendingPlayerProfilex) {
                        SchedulerUtil.runForEntityLater(
                           this.entity, () -> this.packetManipulator.sendRemovePlayerProfile(this.entityInfo, Collections.singleton(player)), FAKE_PLAYER_TAB_LIST_REMOVE_DELAY
                        );
                     }
                  }
               }
            );
         } else {
            boolean sendingPlayerProfile = !this.entityInfo.getEntityUniqueId().equals(this.entityInfo.getEntity().getUniqueId())
               && this.entityInfo.getEntity() instanceof Player;
            if (sendingPlayerProfile) {
               this.packetManipulator.sendAddPlayerProfile(this.entityInfo, Collections.singleton(player));
            }

            this.packetManipulator.showEntity(this.entityInfo, player);
            if (sendingPlayerProfile) {
               SchedulerUtil.runForEntityLater(
                  this.entity, () -> this.packetManipulator.sendRemovePlayerProfile(this.entityInfo, Collections.singleton(player)), FAKE_PLAYER_TAB_LIST_REMOVE_DELAY
               );
            }
         }
      }
   }

   @Override
   public void removeTracking(@NotNull Player player, boolean sendPackets) {
      if (!this.trackingPlayers.contains(player)) {
         throw new IllegalArgumentException("Cannot stop player from tracking entity, they weren't viewing in the first place");
      } else {
         this.trackingPlayers.remove(player);
         if (sendPackets) {
            this.packetManipulator.hideEntity(this.entityInfo, player);
         }
      }
   }

   @Override
   public void removeAllTracking(boolean sendPackets) {
      if (sendPackets && !this.trackingPlayers.isEmpty()) {
         this.packetManipulator.hideEntity(this.entityInfo, this.trackingPlayers);
      }

      this.trackingPlayers.clear();
   }

   @Override
   public int getTrackingPlayerCount() {
      return this.trackingPlayers.size();
   }
}


