package org.envel.immersiveportalspaperized.bukkit.nms;

import org.bukkit.Bukkit;
import org.envel.immersiveportalspaperized.shared.util.ReflectionUtil;

/**
 * CraftBukkitClassUtil.
 */
public class CraftBukkitClassUtil {
   private static final String CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();

   public static Class<?> findCraftBukkitClass(String name) {
      return ReflectionUtil.findClass(CRAFTBUKKIT_PACKAGE + "." + name);
   }
}


