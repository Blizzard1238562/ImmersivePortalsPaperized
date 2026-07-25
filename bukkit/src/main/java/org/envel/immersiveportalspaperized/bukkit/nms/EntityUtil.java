package org.envel.immersiveportalspaperized.bukkit.nms;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityUtil {
   @NotNull
   public static WrappedDataWatcher getActualDataWatcher(@NotNull Entity entity) {
      return WrappedDataWatcher.getEntityWatcher(entity);
   }

   @Nullable
   public static PacketContainer getRawEntitySpawnPacket(@NotNull Entity entity) {
      if (!(entity instanceof EnderDragonPart) && !(entity instanceof Marker)) {
         PacketContainer spawnPacket = new PacketContainer(Server.SPAWN_ENTITY);
         spawnPacket.getIntegers().write(0, entity.getEntityId());
         spawnPacket.getEntityTypeModifier().write(0, entity.getType());
         return spawnPacket;
      } else {
         return null;
      }
   }
}
