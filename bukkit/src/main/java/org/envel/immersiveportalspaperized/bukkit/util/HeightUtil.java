package org.envel.immersiveportalspaperized.bukkit.util;

import org.bukkit.World;

/**
 * HeightUtil.
 */
public class HeightUtil {
   public static int getMaxHeight(World world) {
      return world.getMaxHeight();
   }

   public static int getMinHeight(World world) {
      return world.getMinHeight();
   }
}


