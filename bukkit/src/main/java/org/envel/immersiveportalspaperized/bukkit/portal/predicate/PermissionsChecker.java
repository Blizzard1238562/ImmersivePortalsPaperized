package org.envel.immersiveportalspaperized.bukkit.portal.predicate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.NotNull;

/**
 * PermissionsChecker.
 */
public class PermissionsChecker implements PortalPredicate {
   private final String basePath;

   public PermissionsChecker(String basePath) {
      this.basePath = basePath;
   }

   @Override
   public boolean test(@NotNull ImmersivePortal portal, @NotNull Player player) {
      PluginManager pm = Bukkit.getPluginManager();
      String permName = this.basePath + ((IPortal)portal).getPermissionPath();
      Permission permission = pm.getPermission(permName);
      if (permission == null) {
         permission = new Permission(permName, PermissionDefault.TRUE);
         pm.addPermission(permission);
      }

      return player.hasPermission(this.basePath) && player.hasPermission(permission);
   }
}


