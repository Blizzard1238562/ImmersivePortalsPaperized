package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.nms.AnimationType;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.NotNull;

/**
 * IEntityTracker.
 */
public interface IEntityTracker {
   void addTracking(@NotNull Player player);

   @NotNull
   EntityInfo getEntityInfo();

   @NotNull
   IPortal getPortal();

   void removeTracking(@NotNull Player player, boolean sendPackets);

   /**
    * Stops tracking for every player currently tracking this entity in one go, optionally
    * hiding the fake entity for all of them first. Used by {@link EntityTrackingManager#clearPortal}
    * when a portal is torn down and there's no single player to target - looping
    * {@link #removeTracking} per player isn't an option there since that both needs a
    * pre-existing per-player check and would send one packet per player instead of a single
    * batched hide-entity packet.
    */
   void removeAllTracking(boolean sendPackets);

   int getTrackingPlayerCount();

   void update();

   void onAnimation(@NotNull AnimationType animationType);

   void onPickup(@NotNull EntityInfo pickedUp);

   public interface Factory {
      IEntityTracker create(Entity entity, IPortal portal);
   }
}


