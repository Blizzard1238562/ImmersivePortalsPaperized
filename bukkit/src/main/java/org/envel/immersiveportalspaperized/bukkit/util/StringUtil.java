package org.envel.immersiveportalspaperized.bukkit.util;

import org.bukkit.Location;

/**
 * StringUtil.
 */
public class StringUtil {
   public static String blockLocationToString(Location location) {
      return String.format(
         "(%d, %d, %d, %s)",
         location.getBlockX(),
         location.getBlockY(),
         location.getBlockZ(),
         location.getWorld() == null ? "null" : location.getWorld().getName()
      );
   }

   public static String locationToString(Location location) {
      return String.format(
         "(%.02f, %.02f, %.02f, %s)", location.getX(), location.getY(), location.getZ(), location.getWorld() == null ? "null" : location.getWorld().getName()
      );
   }
}


