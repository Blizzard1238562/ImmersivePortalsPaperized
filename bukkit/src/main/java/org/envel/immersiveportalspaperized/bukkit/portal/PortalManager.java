package org.envel.immersiveportalspaperized.bukkit.portal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.IPortalPredicateManager;
import org.envel.immersiveportalspaperized.bukkit.util.StringUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class PortalManager implements IPortalManager {
   private final Logger logger;
   private final IPortalPredicateManager predicateManager;
   private final IPortalActivityManager portalActivityManager;
   private final MiscConfig miscConfig;
   private final Map<Location, Set<IPortal>> portals = new HashMap<>();
   private final Map<UUID, IPortal> portalsById = new HashMap<>();

   @Inject
   public PortalManager(Logger logger, IPortalPredicateManager predicateManager, IPortalActivityManager portalActivityManager, MiscConfig miscConfig) {
      this.logger = logger;
      this.predicateManager = predicateManager;
      this.portalActivityManager = portalActivityManager;
      this.miscConfig = miscConfig;
   }

   @Override
   public Collection<IPortal> getAllPortals() {
      return this.portalsById.values();
   }

   @Override
   public Collection<IPortal> getPortalsAt(Location originLoc) {
      Set<IPortal> portalsAtLoc = this.portals.get(originLoc);
      return (Collection<IPortal>)(portalsAtLoc == null ? Collections.emptyList() : portalsAtLoc);
   }

   @Override
   public IPortal getPortalById(@Nullable UUID id) {
      return this.portalsById.get(id);
   }

   @Override
   public IPortal findClosestPortal(@NotNull Location position, double maximumDistance, Predicate<IPortal> predicate) {
      IPortal currentClosest = null;
      double currentClosestDistance = maximumDistance;

      for (Entry<Location, Set<IPortal>> entry : this.portals.entrySet()) {
         Location portalPos = entry.getKey();
         if (portalPos.getWorld() == position.getWorld()) {
            double distance = portalPos.distance(position);
            if (!(distance >= currentClosestDistance)) {
               for (IPortal portal : entry.getValue()) {
                  if (predicate.test(portal)) {
                     currentClosest = portal;
                     currentClosestDistance = distance;
                     break;
                  }
               }
            }
         }
      }

      return currentClosest;
   }

   @NotNull
   @Override
   public Collection<IPortal> findActivatablePortals(@NotNull Player player) {
      Location playerLoc = player.getLocation();
      double activationDistance = this.miscConfig.getPortalActivationDistance();
      double activationDistanceSquared = activationDistance * activationDistance;
      List<IPortal> result = new ArrayList<>();

      for (Entry<Location, Set<IPortal>> entry : this.portals.entrySet()) {
         Location portalLoc = entry.getKey();
         if (portalLoc.getWorld() == playerLoc.getWorld() && portalLoc.distanceSquared(playerLoc) < activationDistanceSquared) {
            for (IPortal portal : entry.getValue()) {
               if (this.predicateManager.isActivatable(portal, player)) {
                  result.add(portal);
               }
            }
         }
      }

      return result;
   }

   @Override
   public void registerPortal(@NotNull IPortal portal) {
      this.logger.fine("Registering portal with origin position %s", portal.getOriginPos());
      Location originLoc = portal.getOriginPos().getLocation();
      if (this.miscConfig.isPreventDuplicatePortals()) {
         Set<IPortal> existing = this.portals.get(originLoc);
         if (existing != null) {
            for (IPortal existingPortal : existing) {
               if (existingPortal.getDestPos().equals(portal.getDestPos())) {
                  this.logger
                     .fine(
                        "Anti-Dupe: rejected duplicate portal at origin %s → dest %s (id=%s)",
                        StringUtil.locationToString(originLoc),
                        portal.getDestPos(),
                        portal.getId()
                     );
                  return;
               }
            }
         }
      }

      if (!this.portals.containsKey(originLoc)) {
         this.portals.put(originLoc, new HashSet<>());
      }

      this.portalsById.put(portal.getId(), portal);
      this.portals.get(originLoc).add(portal);
   }

   @Override
   public int removePortalsAt(@NotNull Location originLoc) {
      Set<IPortal> portalsRemoved = this.portals.remove(originLoc);
      if (portalsRemoved == null) {
         return 0;
      } else {
         for (IPortal portal : portalsRemoved) {
            this.portalsById.remove(portal.getId());
         }

         this.logger.fine("Unregistering %d portal(s) at position %s", portalsRemoved.size(), StringUtil.locationToString(originLoc));
         return portalsRemoved.size();
      }
   }

   @Override
   public boolean removePortal(@NotNull IPortal portal) {
      this.logger.fine("Unregistering portal at position %s", StringUtil.locationToString(portal.getOriginPos().getLocation()));
      Set<IPortal> portalsAtLoc = this.portals.get(portal.getOriginPos().getLocation());
      if (portalsAtLoc == null) {
         return false;
      } else {
         boolean wasRemoved = portalsAtLoc.remove(portal);
         if (portalsAtLoc.isEmpty()) {
            this.portals.remove(portal.getOriginPos().getLocation());
         }

         this.portalsById.remove(portal.getId());
         return wasRemoved;
      }
   }

   @Override
   public boolean removePortalById(@NotNull UUID id) {
      IPortal removed = this.portalsById.remove(id);
      if (removed == null) {
         return false;
      } else {
         this.removePortal(removed);
         return true;
      }
   }

   @Override
   public void onReload() {
      this.portalActivityManager.resetActivity();
   }
}
