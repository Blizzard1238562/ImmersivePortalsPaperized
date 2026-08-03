package org.envel.immersiveportalspaperized.bukkit.portal.spawning;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Orientable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.bukkit.config.PortalSpawnConfig;
import org.envel.immersiveportalspaperized.bukkit.config.WorldLink;
import org.envel.immersiveportalspaperized.bukkit.portal.blend.IDimensionBlendManager;
import org.envel.immersiveportalspaperized.bukkit.util.MaterialUtil;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * PortalSpawner.
 */
@Singleton
public class PortalSpawner implements IPortalSpawner {
   private static final double BORDER_PADDING = 10.0;
   private final JavaPlugin pl;
   private final PortalSpawnConfig config;
   private final Logger logger;
   private final ExistingPortalChecker existingPortalChecker;
   private final NewPortalChecker newPortalChecker;
   private final IDimensionBlendManager dimensionBlendManager;

   @Inject
   public PortalSpawner(
      JavaPlugin pl,
      PortalSpawnConfig config,
      Logger logger,
      ExistingPortalChecker existingPortalChecker,
      NewPortalChecker newPortalChecker,
      IDimensionBlendManager dimensionBlendManager
   ) {
      this.pl = pl;
      this.config = config;
      this.logger = logger;
      this.existingPortalChecker = existingPortalChecker;
      this.newPortalChecker = newPortalChecker;
      this.dimensionBlendManager = dimensionBlendManager;
   }

   @Override
   public boolean findAndSpawnDestination(@NotNull Location originPosition, @NotNull Vector originSize, Consumer<PortalSpawnPosition> onFinish) {
      World originWorld = originPosition.getWorld();
      if (originWorld == null) {
         this.logger.fine("Unable to find destination - origin position has no valid world");
         return false;
      }

      WorldLink link = this.config.getWorldLink(originWorld);
      if (link == null) {
         this.logger.fine("Unable to find world link for lit portal at origin position %s with size %s", originPosition, originSize);
         return false;
      } else {
         Location rawDestPos = link.moveFromOriginWorld(originPosition);
         Location destinationPosition = this.limitByWorldBorder(rawDestPos);
         this.logger.fine("Preferred destination position: %s", destinationPosition.toVector());
         PortalSpawningContext context = new PortalSpawningContext(link, destinationPosition, originSize);
         this.logger.fine("Searching for existing position");
         this.startAsyncCheck(context, this.existingPortalChecker, existingPosition -> {
            if (existingPosition != null) {
               this.logger.fine("Creating with existing position");
               this.spawnPortal(existingPosition, originPosition, () -> onFinish.accept(existingPosition));
            } else {
               this.logger.fine("Searching for new position");
               this.startAsyncCheck(context, this.newPortalChecker, newSpawnPos -> {
                  PortalSpawnPosition finalSpawnPos = newSpawnPos;
                  if (newSpawnPos == null) {
                     this.logger.warning("Unable to find destination for a portal. This shouldn't happen really");
                     finalSpawnPos = new PortalSpawnPosition(destinationPosition, originSize, PortalDirection.EAST);
                  }

                  this.logger.fine("Creating with new position");
                  PortalSpawnPosition finalSpawnPosToUse = finalSpawnPos;
                  this.spawnPortal(finalSpawnPosToUse, originPosition, () -> onFinish.accept(finalSpawnPosToUse));
               });
            }
         });
         return true;
      }
   }

   private void startAsyncCheck(PortalSpawningContext context, IChunkChecker chunkChecker, Consumer<PortalSpawnPosition> onFinish) {
      new AsyncPortalChecker(context, chunkChecker, onFinish, this.pl, this.logger, this.config);
   }

   private Location limitByWorldBorder(Location location) {
      WorldBorder border = Objects.requireNonNull(location.getWorld()).getWorldBorder();
      double paddedSize = Math.max(border.getSize() / 2.0 - BORDER_PADDING, 1.0);
      this.logger.finer("Padded size %s", paddedSize);
      Location borderRelativePos = location.clone().subtract(border.getCenter());
      borderRelativePos.setX(Math.min(paddedSize, borderRelativePos.getX()));
      borderRelativePos.setX(Math.max(-paddedSize, borderRelativePos.getX()));
      borderRelativePos.setZ(Math.min(paddedSize, borderRelativePos.getZ()));
      borderRelativePos.setZ(Math.max(-paddedSize, borderRelativePos.getZ()));
      borderRelativePos.add(border.getCenter());
      return borderRelativePos;
   }

   private void spawnPortal(PortalSpawnPosition position, Location originPos, Runnable onComplete) {
      SchedulerUtil.runAtLocation(position.getPosition(), () -> {
         if (this.config.isDimensionBlendEnabled()) {
            this.dimensionBlendManager.performBlend(originPos.clone().add(position.getSize().clone().multiply(0.5)), position.getPosition());
         }

         Vector size = position.getSize().clone().add(new Vector(1.0, 1.0, 0.0));
         PortalDirection direction = position.getDirection();

         for (int x = 0; x <= size.getX(); x++) {
            for (int y = 0; y <= size.getY(); y++) {
               Vector frameRelativePos = new Vector(x, y, 0.0);
               Location blockPos = position.getPosition().clone().add(position.getDirection().swapVector(frameRelativePos));
               boolean isFrameBlock = x == 0 || x == size.getX() || y == 0 || y == size.getY();
               BlockState state = blockPos.getBlock().getState();
               state.setType(isFrameBlock ? Material.OBSIDIAN : MaterialUtil.PORTAL_MATERIAL);
               if (!isFrameBlock && state.getBlockData() instanceof Orientable orientable) {
                  orientable.setAxis(direction != PortalDirection.EAST && direction != PortalDirection.WEST ? Axis.X : Axis.Z);
                  state.setBlockData(orientable);
               }

               state.update(true, false);
            }
         }

         if (onComplete != null) {
            SchedulerUtil.runTask(onComplete);
         }
      });
   }
}


