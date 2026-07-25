package org.envel.immersiveportalspaperized.bukkit.nms;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class NBTTagUtil {
   private static final NamespacedKey MARKER_KEY = new NamespacedKey("immersiveportalspaperized", "marker");
   private static final String MARKER_VALUE = "marked";

   @NotNull
   public static ItemStack addMarkerTag(@NotNull ItemStack item, @NotNull String name) {
      ItemStack newItem = item.clone();
      ItemMeta meta = newItem.getItemMeta();
      if (meta == null) {
         return newItem;
      } else {
         meta.getPersistentDataContainer().set(MARKER_KEY, PersistentDataType.STRING, "marked_" + name);
         newItem.setItemMeta(meta);
         return newItem;
      }
   }

   public static boolean hasMarkerTag(@NotNull ItemStack item, @NotNull String name) {
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return false;
      } else {
         String value = (String)meta.getPersistentDataContainer().get(MARKER_KEY, PersistentDataType.STRING);
         return value != null && value.equals("marked_" + name);
      }
   }
}
