package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.nms.AnimationType;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.NotNull;

public interface IEntityTracker {
   void addTracking(@NotNull Player player);

   @NotNull
   EntityInfo getEntityInfo();

   @NotNull
   IPortal getPortal();

   void removeTracking(@NotNull Player player, boolean sendPackets);

   int getTrackingPlayerCount();

   void update();

   void onAnimation(@NotNull AnimationType animationType);

   void onPickup(@NotNull EntityInfo pickedUp);

   public interface Factory {
      IEntityTracker create(Entity entity, IPortal portal);
   }
}
