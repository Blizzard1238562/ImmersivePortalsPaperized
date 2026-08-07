package org.envel.immersiveportalspaperized.bukkit.portal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityTrackingManager;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.IPortalPredicateManager;
import org.envel.immersiveportalspaperized.bukkit.util.StringUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * PortalManager.
 */
@Singleton
public class PortalManager implements IPortalManager {
   private final Logger logger;
   private final IPortalPredicateManager predicateManager;
   private final IPortalActivityManager portalActivityManager;
   private final MiscConfig miscConfig;
   private final EntityTrackingManager entityTrackingManager;
   private final Map<Location, Set<IPortal>> portals = new HashMap<>();
   private final Map<UUID, IPortal> portalsById = new HashMap<>();
   private final Map<World, Map<Long, Set<IPortal>>> portalsByChunk = new HashMap<>();

   @Inject
   public PortalManager(Logger logger, IPortalPredicateManager predicateManager, IPortalActivityManager portalActivityManager, MiscConfig miscConfig, EntityTrackingManager entityTrackingManager) {
      this.logger = logger;
      this.predicateManager = predicateManager;
      this.portalActivityManager = portalActivityManager;
      this.miscConfig = miscConfig;
      this.entityTrackingManager = entityTrackingManager;
   }

   private static long chunkKey(int chunkX, int chunkZ) {
      return ((long)chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
   }

   private static int chunkX(Location location) {
      return location.getBlockX() >> 4;
   }

   private static int chunkZ(Location location) {
      return location.getBlockZ() >> 4;
   }

   private Map<Long, Set<IPortal>> getChunkMap(World world) {
      return this.portalsByChunk.computeIfAbsent(world, k -> new HashMap<>());
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
      double currentClosestDistanceSquared = maximumDistance * maximumDistance;
      World world = position.getWorld();
      if (world == null) {
         return null;
      } else if (Double.isInfinite(maximumDistance)) {
         // Chunk-radius scanning below assumes a finite radius; (int) casting an infinite
         // distance yields Integer.MAX_VALUE and would iterate ~2^64 chunk cells. Fall back
         // to a direct scan over all known portals in this world instead.
         for (IPortal portal : this.portalsById.values()) {
            Location portalPos = portal.getOriginPos().getLocation();
            if (portalPos.getWorld() != world) {
               continue;
            }

            double distanceSquared = portalPos.distanceSquared(position);
            if (!(distanceSquared >= currentClosestDistanceSquared) && predicate.test(portal)) {
               currentClosest = portal;
               currentClosestDistanceSquared = distanceSquared;
            }
         }

         return currentClosest;
      } else {
         int centerChunkX = position.getBlockX() >> 4;
         int centerChunkZ = position.getBlockZ() >> 4;
         int maxChunkRadius = (int)Math.ceil(maximumDistance / 16.0);
         Map<Long, Set<IPortal>> chunkMap = this.getChunkMap(world);

         for (int dx = -maxChunkRadius; dx <= maxChunkRadius; dx++) {
            for (int dz = -maxChunkRadius; dz <= maxChunkRadius; dz++) {
               Set<IPortal> chunkPortals = chunkMap.get(chunkKey(centerChunkX + dx, centerChunkZ + dz));
               if (chunkPortals == null) {
                  continue;
               }

               for (IPortal portal : chunkPortals) {
                  Location portalPos = portal.getOriginPos().getLocation();
                  double distanceSquared = portalPos.distanceSquared(position);
                  if (!(distanceSquared >= currentClosestDistanceSquared) && predicate.test(portal)) {
                     currentClosest = portal;
                     currentClosestDistanceSquared = distanceSquared;
                  }
               }
            }
         }

         return currentClosest;
      }
   }

   @NotNull
   @Override
   public Collection<IPortal> findActivatablePortals(@NotNull Player player) {
      Location playerLoc = player.getLocation();
      double activationDistance = this.miscConfig.getPortalActivationDistance();
      double activationDistanceSquared = activationDistance * activationDistance;
      List<IPortal> result = new ArrayList<>();
      World world = playerLoc.getWorld();
      if (world == null) {
         return result;
      } else {
         int centerChunkX = playerLoc.getBlockX() >> 4;
         int centerChunkZ = playerLoc.getBlockZ() >> 4;
         int chunkRadius = (int)Math.ceil(activationDistance / 16.0);
         Map<Long, Set<IPortal>> chunkMap = this.getChunkMap(world);

         for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
               Set<IPortal> chunkPortals = chunkMap.get(chunkKey(centerChunkX + dx, centerChunkZ + dz));
               if (chunkPortals == null) {
                  continue;
               }

               for (IPortal portal : chunkPortals) {
                  Location portalLoc = portal.getOriginPos().getLocation();
                  if (portalLoc.getWorld() == world && portalLoc.distanceSquared(playerLoc) < activationDistanceSquared && this.predicateManager.isActivatable(portal, player)) {
                     result.add(portal);
                  }
               }
            }
         }

         return result;
      }
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
                        "Anti-Dupe: rejected duplicate portal at origin %s â†’ dest %s (id=%s)",
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
      World world = originLoc.getWorld();
      if (world != null) {
         this.getChunkMap(world).computeIfAbsent(chunkKey(chunkX(originLoc), chunkZ(originLoc)), k -> new HashSet<>()).add(portal);
      }
   }

   @Override
   public int removePortalsAt(@NotNull Location originLoc) {
      Set<IPortal> portalsRemoved = this.portals.remove(originLoc);
      if (portalsRemoved == null) {
         return 0;
      } else {
         World world = originLoc.getWorld();
         if (world != null) {
            Map<Long, Set<IPortal>> chunkMap = this.portalsByChunk.get(world);
            if (chunkMap != null) {
               long key = chunkKey(chunkX(originLoc), chunkZ(originLoc));
               Set<IPortal> chunkSet = chunkMap.get(key);
               if (chunkSet != null) {
                  chunkSet.removeAll(portalsRemoved);
                  if (chunkSet.isEmpty()) {
                     chunkMap.remove(key);
                  }
               }
            }
         }

         for (IPortal portal : portalsRemoved) {
            this.portalsById.remove(portal.getId());
            this.entityTrackingManager.clearPortal(portal);
         }

         this.logger.fine("Unregistering %d portal(s) at position %s", portalsRemoved.size(), StringUtil.locationToString(originLoc));
         return portalsRemoved.size();
      }
   }

   @Override
   public boolean removePortal(@NotNull IPortal portal) {
      this.logger.fine("Unregistering portal at position %s", StringUtil.locationToString(portal.getOriginPos().getLocation()));
      Location originLoc = portal.getOriginPos().getLocation();
      Set<IPortal> portalsAtLoc = this.portals.get(originLoc);
      if (portalsAtLoc == null) {
         return false;
      } else {
         boolean wasRemoved = portalsAtLoc.remove(portal);
         if (portalsAtLoc.isEmpty()) {
            this.portals.remove(originLoc);
         }

         this.portalsById.remove(portal.getId());
         if (wasRemoved) {
            World world = originLoc.getWorld();
            if (world != null) {
               Map<Long, Set<IPortal>> chunkMap = this.portalsByChunk.get(world);
               if (chunkMap != null) {
                  long key = chunkKey(chunkX(originLoc), chunkZ(originLoc));
                  Set<IPortal> chunkSet = chunkMap.get(key);
                  if (chunkSet != null) {
                     chunkSet.remove(portal);
                     if (chunkSet.isEmpty()) {
                        chunkMap.remove(key);
                     }
                  }
               }
            }
            this.entityTrackingManager.clearPortal(portal);
         }
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
      this.portalsByChunk.clear();
   }
}


