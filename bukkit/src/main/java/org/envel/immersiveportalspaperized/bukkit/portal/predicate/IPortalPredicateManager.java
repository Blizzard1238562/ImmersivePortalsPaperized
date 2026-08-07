package org.envel.immersiveportalspaperized.bukkit.portal.predicate;

import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;

/**
 * IPortalPredicateManager.
 */
public interface IPortalPredicateManager {
   void addActivationPredicate(PortalPredicate predicate);

   boolean removeActivationPredicate(PortalPredicate predicate);

   void addViewPredicate(PortalPredicate predicate);

   boolean removeViewPredicate(PortalPredicate predicate);

   void addTeleportPredicate(PortalPredicate predicate);

   boolean removeTeleportPredicate(PortalPredicate predicate);

   boolean isActivatable(IPortal portal, Player player);

   boolean isViewable(IPortal portal, Player player);

   boolean canTeleport(IPortal portal, Player player);
}


