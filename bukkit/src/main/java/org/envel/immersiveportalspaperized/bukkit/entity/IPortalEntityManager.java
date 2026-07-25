package org.envel.immersiveportalspaperized.bukkit.entity;

import java.util.Collection;
import org.bukkit.entity.Entity;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;

public interface IPortalEntityManager {
   Collection<Entity> getOriginEntities();

   Collection<Entity> getDestinationEntities();

   void update(int ticksSinceActivated);

   public interface Factory {
      IPortalEntityManager create(IPortal portal, boolean requireDestination);
   }
}
