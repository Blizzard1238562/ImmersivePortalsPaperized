package org.envel.immersiveportalspaperized.bukkit.util;

import org.bukkit.Bukkit;

public class VersionUtil {
   public static String getCurrentVersion() {
      return Bukkit.getServer().getMinecraftVersion();
   }
}
