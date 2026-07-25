package org.envel.immersiveportalspaperized.bukkit.portal.predicate;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ActivationDistance implements PortalPredicate {
   private final MiscConfig miscConfig;

   @Inject
   public ActivationDistance(MiscConfig miscConfig) {
      this.miscConfig = miscConfig;
   }

   @Override
   public boolean test(@NotNull ImmersivePortal portal, @NotNull Player player) {
      Location portalOrigin = portal.getOriginPos().getLocation();
      Location playerPos = player.getLocation();
      return portalOrigin.getWorld() != playerPos.getWorld() ? false : playerPos.distance(portalOrigin) < this.miscConfig.getPortalActivationDistance();
   }
}
