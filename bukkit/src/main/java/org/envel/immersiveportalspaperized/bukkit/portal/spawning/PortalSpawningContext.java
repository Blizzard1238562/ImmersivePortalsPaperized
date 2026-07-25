package org.envel.immersiveportalspaperized.bukkit.portal.spawning;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.config.WorldLink;

@Getter
public class PortalSpawningContext {
   private final WorldLink worldLink;
   private final Location preferredLocation;
   private final Vector size;

   public PortalSpawningContext(WorldLink worldLink, Location preferredLocation, Vector size) {
      this.worldLink = worldLink;
      this.preferredLocation = preferredLocation;
      this.size = size;
   }
}
