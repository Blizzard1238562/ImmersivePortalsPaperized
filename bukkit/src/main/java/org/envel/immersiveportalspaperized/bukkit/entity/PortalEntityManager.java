package org.envel.immersiveportalspaperized.bukkit.entity;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.math.PortalTransformations;
import org.envel.immersiveportalspaperized.bukkit.net.IPortalClient;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerData;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.IPortalPredicateManager;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.assistedinject.Assisted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.RequestException;
import org.envel.immersiveportalspaperized.shared.net.requests.TeleportRequest;

public class PortalEntityManager implements IPortalEntityManager {
   private final IPortal portal;
   private final MiscConfig miscConfig;
   private final RenderConfig renderConfig;
   private final IPortalPredicateManager predicateManager;
   private final Logger logger;
   private final IPortalClient portalClient;
   private final Set<Player> alreadyTeleporting = new HashSet<>();
   private final JavaPlugin pl;
   private final IEntityFinder entityFinder;
   private final IPlayerDataManager playerDataManager;
   private final boolean requireDestination;
   @Getter
   private volatile Collection<Entity> destinationEntities = ConcurrentHashMap.newKeySet();
   private volatile Map<Entity, Location> originEntities = new ConcurrentHashMap<>();
   private final AtomicBoolean isFetchingDest = new AtomicBoolean(false);
   private final AtomicBoolean isFetchingOrigin = new AtomicBoolean(false);
   private final Set<Entity> alreadyTeleportingLocal = ConcurrentHashMap.newKeySet();

   @Inject
   public PortalEntityManager(
      @Assisted IPortal portal,
      @Assisted boolean requireDestination,
      MiscConfig miscConfig,
      RenderConfig renderConfig,
      IPortalPredicateManager predicateManager,
      Logger logger,
      IPortalClient portalClient,
      JavaPlugin pl,
      IEntityFinder entityFinder,
      IPlayerDataManager playerDataManager
   ) {
      this.portal = portal;
      this.requireDestination = requireDestination;
      this.miscConfig = miscConfig;
      this.renderConfig = renderConfig;
      this.predicateManager = predicateManager;
      this.logger = logger;
      this.portalClient = portalClient;
      this.pl = pl;
      this.entityFinder = entityFinder;
      this.playerDataManager = playerDataManager;
   }

   @Override
   public void update(int ticksSinceActivated) {
      if (ticksSinceActivated % this.miscConfig.getEntityCheckInterval() == 0) {
         this.updateEntityLists();
      }

      this.handleTeleportation();
   }

   private void updateEntityLists() {
      if (SchedulerUtil.isFolia()) {
         this.updateEntityListsFolia();
      } else {
         if (this.requireDestination) {
            this.destinationEntities = this.getNearbyEntities(this.destinationEntities, this.portal.getDestPos());
         }

         Map<Entity, Location> oldOriginEntities = this.originEntities;
         this.originEntities = new HashMap<>();
         this.getNearbyEntities(this.portal.getOriginPos(), entity -> {
            Location oldLocation = oldOriginEntities == null ? null : oldOriginEntities.get(entity);
            this.originEntities.put(entity, oldLocation != null ? oldLocation : entity.getLocation());
         });
      }
   }

   private void updateEntityListsFolia() {
      if (this.requireDestination && this.isFetchingDest.compareAndSet(false, true)) {
         Location loc = this.portal.getDestPos().getLocation();
         World world = loc.getWorld();
         if (world != null) {
            SchedulerUtil.runAtLocation(
               loc,
               () -> {
                  try {
                     Collection<Entity> nearby = ConcurrentHashMap.newKeySet();
                     nearby.addAll(
                        this.entityFinder.getNearbyEntities(null, loc, this.renderConfig.getMaxXZ(), this.renderConfig.getMaxY(), this.renderConfig.getMaxXZ())
                     );
                     this.destinationEntities = nearby;
                  } catch (Exception e) {
                     this.logger.warning("Error fetching destination entities: %s", e.getMessage());
                  } finally {
                     this.isFetchingDest.set(false);
                  }
               }
            );
         } else {
            this.isFetchingDest.set(false);
         }
      }

      if (this.isFetchingOrigin.compareAndSet(false, true)) {
         Location loc = this.portal.getOriginPos().getLocation();
         World world = loc.getWorld();
         if (world != null) {
            SchedulerUtil.runAtLocation(loc, () -> {
               try {
                  Map<Entity, Location> newOrigin = new ConcurrentHashMap<>();
                  this.getNearbyEntities(this.portal.getOriginPos(), entity -> {
                     Location oldLocation = this.originEntities.get(entity);
                     newOrigin.put(entity, oldLocation != null ? oldLocation : entity.getLocation());
                  });
                  this.originEntities = newOrigin;
               } catch (Exception e) {
                  this.logger.warning("Error fetching origin entities: %s", e.getMessage());
               } finally {
                  this.isFetchingOrigin.set(false);
               }
            });
         } else {
            this.isFetchingOrigin.set(false);
         }
      }
   }

   private void handleTeleportation() {
      List<Entity> toRemove = new ArrayList<>();
      boolean allowsNonPlayerTeleportation = this.portal.allowsNonPlayerTeleportation();
      boolean isCrossServer = this.portal.isCrossServer();

      for (Entry<Entity, Location> entry : this.originEntities.entrySet()) {
         Entity entity = entry.getKey();
         if (entity instanceof Player || allowsNonPlayerTeleportation && !isCrossServer) {
            Location lastPosition = entry.getValue();
            Location currentPosition = entity.getLocation();
            if (lastPosition != null) {
               boolean didWalkThroughPortal = this.portal
                  .getTransformations()
                  .createIntersectionChecker(lastPosition.toVector())
                  .checkIfIntersects(currentPosition.toVector());
               if (didWalkThroughPortal && this.checkCanTeleport(entity)) {
                  if (isCrossServer) {
                     if (entity instanceof Player player) {
                        this.teleportCrossServer(player);
                     }
                  } else {
                     this.teleportLocal(entity);
                  }

                  toRemove.add(entity);
                  continue;
               }
            }

            entry.setValue(currentPosition);
         }
      }

      toRemove.forEach(this.originEntities::remove);
   }

   @Override
   public Collection<Entity> getOriginEntities() {
      return this.originEntities.keySet();
   }

   private Collection<Entity> getNearbyEntities(@Nullable Collection<Entity> existing, PortalPosition position) {
      return this.entityFinder
         .getNearbyEntities(existing, position.getLocation(), this.renderConfig.getMaxXZ(), this.renderConfig.getMaxY(), this.renderConfig.getMaxXZ());
   }

   private void getNearbyEntities(PortalPosition position, Consumer<Entity> sendTo) {
      this.entityFinder
         .getNearbyEntities(position.getLocation(), this.renderConfig.getMaxXZ(), this.renderConfig.getMaxY(), this.renderConfig.getMaxXZ(), sendTo);
   }

   private boolean checkCanTeleport(Entity entity) {
      if (entity.getVehicle() != null) {
         return false;
      } else {
         return entity instanceof Player ? this.predicateManager.canTeleport(this.portal, (Player)entity) : true;
      }
   }

   @NotNull
   private Location limitToBlockHitbox(@NotNull Location preferred) {
      Location flooredPos = MathUtil.floor(preferred);
      Location blockOffset = preferred.clone().subtract(flooredPos);
      if (blockOffset.getZ() > 0.6 && preferred.clone().add(0.0, 0.0, 1.0).getBlock().getType().isSolid()) {
         blockOffset.setZ(0.6);
      }

      if (blockOffset.getX() > 0.6 && preferred.clone().add(1.0, 0.0, 0.0).getBlock().getType().isSolid()) {
         blockOffset.setX(0.6);
      }

      if (blockOffset.getZ() < 0.4 && preferred.clone().add(0.0, 0.0, -1.0).getBlock().getType().isSolid()) {
         blockOffset.setZ(0.4);
      }

      if (blockOffset.getX() < 0.4 && preferred.clone().add(-1.0, 0.0, 0.0).getBlock().getType().isSolid()) {
         blockOffset.setX(0.4);
      }

      this.logger.finer("Fixing position. Floored pos: %s. Block offset: %s", flooredPos.toVector(), blockOffset.toVector());
      return blockOffset.add(flooredPos);
   }

   private void teleportLocal(Entity entity) {
      PortalTransformations transformations = this.portal.getTransformations();
      Location destPos = transformations.moveToDestination(entity.getLocation());
      if (SchedulerUtil.isFolia()) {
         if (entity instanceof Player) {
            if (this.alreadyTeleportingLocal.contains(entity)) {
               return;
            }

            this.alreadyTeleportingLocal.add(entity);
            Player player = (Player)entity;
            Vector velocity = transformations.rotateToDestination(player.getVelocity());
            SchedulerUtil.runAtLocation(destPos, () -> {
               Location finalDestPos = this.limitToBlockHitbox(destPos.clone());
               SchedulerUtil.runForEntity(player, () -> {
                  this.logger.fine("Teleporting player to position %s", finalDestPos.toVector());
                  player.teleportAsync(finalDestPos).thenAccept(success -> {
                     if (Boolean.TRUE.equals(success)) {
                        player.setVelocity(velocity);
                     }

                     this.alreadyTeleportingLocal.remove(player);
                  }).exceptionally(ex -> {
                     this.alreadyTeleportingLocal.remove(player);
                     return null;
                  });
               });
            });
         } else {
            if (this.alreadyTeleportingLocal.contains(entity)) {
               return;
            }

            this.alreadyTeleportingLocal.add(entity);
            Location finalDestPos = destPos.clone().add(0.0, 0.2, 0.0);
            Vector velocity = transformations.rotateToDestination(entity.getVelocity());
            int entityId = entity.getEntityId();
            EntityType entityType = entity.getType();
            boolean handlePassengers = entity.getWorld() != finalDestPos.getWorld();
            List<Entity> passengers = entity.getPassengers();
            this.logger.fine("Teleporting entity with ID %d and of type %s to position %s", entityId, entityType, finalDestPos.toVector());
            if (handlePassengers) {
               for (Entity passenger : passengers) {
                  entity.removePassenger(passenger);
                  this.teleportLocal(passenger);
               }
            }

            entity.teleportAsync(finalDestPos).thenAccept(success -> {
               if (Boolean.TRUE.equals(success)) {
                  entity.setVelocity(velocity);
                  if (handlePassengers) {
                     passengers.forEach(entity::addPassenger);
                  }
               }

               this.alreadyTeleportingLocal.remove(entity);
            }).exceptionally(ex -> {
               this.alreadyTeleportingLocal.remove(entity);
               return null;
            });
         }
      } else {
         Location nonFoliaDestPos = destPos;
         if (entity instanceof Player) {
            nonFoliaDestPos = this.limitToBlockHitbox(nonFoliaDestPos);
         } else {
            nonFoliaDestPos.add(0.0, 0.2, 0.0);
         }

         Location finalDestPos = nonFoliaDestPos;
         Vector velocity = transformations.rotateToDestination(entity.getVelocity());
         this.logger.fine("Teleporting entity with ID %d and of type %s to position %s", entity.getEntityId(), entity.getType(), finalDestPos.toVector());
         boolean handlePassengers = entity.getWorld() != finalDestPos.getWorld();
         List<Entity> passengers = entity.getPassengers();
         if (handlePassengers) {
            for (Entity passenger : passengers) {
               entity.removePassenger(passenger);
               this.teleportLocal(passenger);
            }
         }

         if (entity instanceof Player && passengers.isEmpty()) {
            entity.teleportAsync(finalDestPos).thenAccept(success -> {
               if (Boolean.TRUE.equals(success)) {
                  entity.setVelocity(velocity);
               }
            });
         } else {
            entity.teleport(finalDestPos);
            entity.setVelocity(velocity);
            if (handlePassengers) {
               passengers.forEach(entity::addPassenger);
            }
         }
      }
   }

   private void teleportCrossServer(Player player) {
      if (!this.alreadyTeleporting.contains(player)) {
         this.alreadyTeleporting.add(player);
         IPlayerData playerData = this.playerDataManager.getPlayerData(player);
         if (playerData == null) {
            this.logger.warning("Player with unregistered data %s", player.getUniqueId());
         } else {
            playerData.freezePortalViews();
            Location destPosition = this.portal.getTransformations().moveToDestination(player.getLocation());
            Vector destVelocity = this.portal.getTransformations().rotateToDestination(player.getVelocity());
            TeleportRequest request = new TeleportRequest();
            request.setDestWorldId(this.portal.getDestPos().getWorldId());
            request.setDestWorldName(this.portal.getDestPos().getWorldName());
            request.setDestServer(this.portal.getDestPos().getServerName());
            request.setPlayerId(player.getUniqueId());
            request.setDestX(destPosition.getX());
            request.setDestY(destPosition.getY());
            request.setDestZ(destPosition.getZ());
            request.setDestVelX(destVelocity.getX());
            request.setDestVelY(destVelocity.getY());
            request.setDestVelZ(destVelocity.getZ());
            request.setFlying(player.isFlying());
            request.setGliding(player.isGliding());
            request.setDestPitch(destPosition.getPitch());
            request.setDestYaw(destPosition.getYaw());
            this.portalClient.sendRequestToProxy(request, response -> {
               try {
                  response.checkForErrors();
                  this.alreadyTeleporting.remove(player);
               } catch (RequestException e) {
                  this.alreadyTeleporting.remove(player);
                  playerData.unfreezePortalViews();
                  if (!this.pl.isEnabled()) {
                     return;
                  }

                  this.logger.warning("An error occurred while attempting to teleport a player across servers: %s", e.getMessage());
               }
            });
         }
      }
   }
}
