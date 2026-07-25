package org.envel.immersiveportalspaperized.bukkit.portal.spawning;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.api.PortalPosition;

@Getter
public class PortalSpawnPosition {
   private final Location position;
   private final Vector size;
   private final PortalDirection direction;

   public PortalSpawnPosition(Location position, Vector size, PortalDirection direction) {
      this.position = position;
      this.size = size;
      this.direction = direction;
   }

   public PortalPosition toPortalPosition() {
      Location centerPos = this.position.clone();
      centerPos.add(this.direction.swapVector(new Vector(1.0, 1.0, 0.5)));
      centerPos.add(this.direction.swapVector(this.size.clone().multiply(0.5)));
      return new PortalPosition(centerPos, this.direction);
   }

   @Override
   public String toString() {
      return String.format(
         "(%d, %d, %d, %s, Size=%s)", this.position.getBlockX(), this.position.getBlockY(), this.position.getBlockZ(), this.direction, this.size
      );
   }
}
