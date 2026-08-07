package org.envel.immersiveportalspaperized.bukkit.events;

import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.config.PortalSpawnConfig;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.portal.spawning.IPortalSpawner;
import org.envel.immersiveportalspaperized.bukkit.portal.spawning.PortalSpawnPosition;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * SpawningEvents.
 */
public class SpawningEvents implements Listener {
   private final IPortalSpawner portalSpawnChecker;
   private final IPortalManager portalManager;
   private final IPortal.Factory portalFactory;
   private final PortalSpawnConfig spawnConfig;
   private final MessageConfig messageConfig;
   private final Logger logger;

   @Inject
   public SpawningEvents(
      IEventRegistrar eventRegistrar,
      IPortalSpawner portalSpawnChecker,
      IPortalManager portalManager,
      IPortal.Factory portalFactory,
      PortalSpawnConfig spawnConfig,
      MessageConfig messageConfig,
      Logger logger
   ) {
      this.portalSpawnChecker = portalSpawnChecker;
      this.portalManager = portalManager;
      this.portalFactory = portalFactory;
      this.spawnConfig = spawnConfig;
      this.messageConfig = messageConfig;
      this.logger = logger;
      eventRegistrar.register(this);
   }

   @EventHandler
   public void onPortalCreate(PortalCreateEvent event) {
      if (!this.spawnConfig.isWorldDisabled(event.getWorld())) {
         if (event.getReason() == CreateReason.FIRE) {
            Vector highPosition = null;
            Vector lowPosition = null;
            List<?> blocks = event.getBlocks();
            if (!blocks.isEmpty()) {
               for (Object obj : blocks) {
                  Block block;
                  if (obj instanceof BlockState) {
                     block = ((BlockState)obj).getBlock();
                  } else {
                     block = (Block)obj;
                  }

                  if (block.getType() != Material.OBSIDIAN) {
                     Vector position = block.getLocation().toVector();
                     if (highPosition == null || MathUtil.greaterThanEq(position, highPosition)) {
                        highPosition = position;
                     }

                     if (lowPosition == null || MathUtil.lessThanEq(position, lowPosition)) {
                        lowPosition = position;
                     }
                  }
               }

               if (highPosition == null) {
                  this.logger.fine("PortalCreateEvent contained no non-obsidian blocks, ignoring");
                  return;
               }

               PortalDirection direction = this.findPortalDirection(highPosition, lowPosition);
               Vector size = this.findPortalSize(highPosition, lowPosition, direction);
               Vector maxSize = this.spawnConfig.getMaxPortalSize();
               if (!MathUtil.lessThanEq(size, maxSize)) {
                  event.setCancelled(true);
                  this.logger.fine("Not spawning portal - too big: %s", size);
                  Player player = event.getEntity() instanceof Player ? (Player)event.getEntity() : null;
                  String msg = this.messageConfig.getWarningMessage(player, "portalTooBig");
                  msg = msg.replace("{size}", String.format("%dx%d", maxSize.getBlockX(), maxSize.getBlockY()));
                  this.sendMessageToLighter(event, msg);
               } else {
                  Location bottomLeftLocation = lowPosition.toLocation(event.getWorld());
                  bottomLeftLocation.subtract(direction.swapVector(new Vector(1.0, 1.0, 0.0)));
                  PortalSpawnPosition originPosition = new PortalSpawnPosition(bottomLeftLocation, size, direction);
                  this.logger.fine("Attempting to spawn portal with origin %s", originPosition);
                  boolean successful = this.portalSpawnChecker
                     .findAndSpawnDestination(bottomLeftLocation, size, destination -> this.registerPortals(originPosition, destination, size));
                  if (!successful) {
                     this.logger.fine("Spawning was unsuccessful, blocking portal blocks from appearing!");
                     Player player = event.getEntity() instanceof Player ? (Player)event.getEntity() : null;
                     this.sendMessageToLighter(event, this.messageConfig.getWarningMessage(player, "noWorldLink"));
                     event.setCancelled(true);
                  }
               }
            }
         }
      }
   }

   private void registerPortals(PortalSpawnPosition origin, PortalSpawnPosition destination, Vector size) {
      IPortal portal = this.portalFactory.create(origin.toPortalPosition(), destination.toPortalPosition(), size, false, UUID.randomUUID(), null, null, true);
      IPortal reversePortal = this.portalFactory
         .create(destination.toPortalPosition(), origin.toPortalPosition(), size, false, UUID.randomUUID(), null, null, true);
      this.portalManager.registerPortal(portal);
      this.portalManager.registerPortal(reversePortal);
   }

   @NotNull
   private Vector findPortalSize(Vector highPosition, Vector lowPosition, PortalDirection direction) {
      Vector portalSize = direction.swapVector(highPosition.clone().subtract(lowPosition));
      portalSize.add(new Vector(1.0, 1.0, 0.0));
      if (!MathUtil.greaterThanEq(portalSize, new Vector(2.0, 3.0, 0.0))) {
         this.logger.warning("PortalCreateEvent called on a portal under 2x3 in size: %s", portalSize);
      }

      return portalSize;
   }

   @NotNull
   private PortalDirection findPortalDirection(Vector highPosition, Vector lowPosition) {
      if (highPosition.getX() == lowPosition.getX()) {
         return PortalDirection.EAST;
      } else if (highPosition.getZ() == lowPosition.getZ()) {
         return PortalDirection.NORTH;
      } else {
         throw new IllegalStateException("Invalid PortalCreateEvent called by Bukkit. Portal was not on a valid plane");
      }
   }

   private void sendMessageToLighter(@NotNull PortalCreateEvent event, @NotNull String message) {
      if (!message.isEmpty()) {
         Entity lighter = event.getEntity();
         if (lighter instanceof Player) {
            lighter.sendMessage(message);
         }
      }
   }
}


