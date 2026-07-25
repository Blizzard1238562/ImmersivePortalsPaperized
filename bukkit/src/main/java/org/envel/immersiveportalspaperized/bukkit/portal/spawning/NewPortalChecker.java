package org.envel.immersiveportalspaperized.bukkit.portal.spawning;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.ChunkPosition;
import org.envel.immersiveportalspaperized.bukkit.config.PortalSpawnConfig;
import org.envel.immersiveportalspaperized.bukkit.config.WorldLink;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class NewPortalChecker implements IChunkChecker {
   private static final PortalDirection[] CHECKED_DIRECTIONS = new PortalDirection[]{PortalDirection.NORTH, PortalDirection.EAST};
   private final IPortalManager portalManager;
   private final PortalSpawnConfig spawnConfig;

   @Inject
   public NewPortalChecker(IPortalManager portalManager, PortalSpawnConfig spawnConfig) {
      this.portalManager = portalManager;
      this.spawnConfig = spawnConfig;
   }

   @Nullable
   @Override
   public PortalSpawnPosition findClosestInChunk(@NotNull ChunkPosition chunk, @NotNull PortalSpawningContext context) {
      PortalSpawnPosition currentClosest = null;
      double closestDistance = Double.POSITIVE_INFINITY;
      WorldLink link = context.getWorldLink();
      Location preferredLocation = context.getPreferredLocation();
      World world = chunk.world;
      int baseX = chunk.x << 4;
      int baseZ = chunk.z << 4;
      Location blockPos = new Location(world, 0.0, 0.0, 0.0);

      for (int y = link.getMinSpawnY(); y < link.getMaxSpawnY(); y++) {
         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               blockPos.setX(baseX + x);
               blockPos.setY(y);
               blockPos.setZ(baseZ + z);
               double distance = blockPos.distance(preferredLocation);
               if (!(distance >= closestDistance)) {
                  for (PortalDirection direction : CHECKED_DIRECTIONS) {
                     if (this.isValidPortalSpawnPosition(blockPos, direction, context.getSize())) {
                        closestDistance = distance;
                        currentClosest = new PortalSpawnPosition(blockPos.clone(), context.getSize(), direction);
                     }
                  }
               }
            }
         }
      }

      return currentClosest;
   }

   public boolean isValidPortalSpawnPosition(Location location, PortalDirection direction, Vector size) {
      int sizeX = (int)size.getX() + 1;
      int sizeY = (int)size.getY() + 1;
      Location temp = location.clone();

      for (int z = -1; z <= 1; z++) {
         for (int x = 0; x <= sizeX; x++) {
            for (int y = 0; y <= sizeY; y++) {
               Vector frameRelativePos = new Vector(x, y, z);
               Vector swapped = direction.swapVector(frameRelativePos);
               temp.setX(location.getX() + swapped.getX());
               temp.setY(location.getY() + swapped.getY());
               temp.setZ(location.getZ() + swapped.getZ());
               Material type = temp.getBlock().getType();
               boolean isFrame = x == 0 || y == 0 || x == sizeX || y == sizeY;
               if (!isFrame && !type.isAir()) {
                  return false;
               }

               if (y == 0 && !type.isSolid()) {
                  return false;
               }
            }
         }
      }

      boolean isFarEnoughSpaced = this.portalManager.findClosestPortal(location, this.spawnConfig.getMinimumPortalSpawnDistance()) == null;
      boolean isInsideWorldBorder = location.getWorld().getWorldBorder().isInside(location);
      return isFarEnoughSpaced && isInsideWorldBorder;
   }
}
