package org.envel.immersiveportalspaperized.bukkit.portal.spawning;

import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.ChunkPosition;
import org.envel.immersiveportalspaperized.bukkit.chunk.generation.IChunkGenerationChecker;
import org.envel.immersiveportalspaperized.bukkit.config.PortalSpawnConfig;
import org.envel.immersiveportalspaperized.bukkit.config.WorldLink;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.util.MaterialUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ExistingPortalChecker implements IChunkChecker {
   private static final double VALIDITY_THRESHOLD = 0.85;
   private static final PortalDirection[] CHECKED_DIRECTIONS = new PortalDirection[]{PortalDirection.NORTH, PortalDirection.EAST};
   private static final Vector[] XZ_CHECK_OFFSETS = new Vector[]{
      new Vector(0.0, 0.0, 0.0), new Vector(1.0, 0.0, 0.0), new Vector(-1.0, 0.0, 0.0), new Vector(0.0, 0.0, -1.0), new Vector(0.0, 0.0, 1.0)
   };
   private final IPortalManager portalManager;
   private final PortalSpawnConfig spawnConfig;
   private final IChunkGenerationChecker generationChecker;

   @Inject
   public ExistingPortalChecker(IPortalManager portalManager, PortalSpawnConfig spawnConfig, IChunkGenerationChecker generationChecker) {
      this.portalManager = portalManager;
      this.spawnConfig = spawnConfig;
      this.generationChecker = generationChecker;
   }

   @Override
   public PortalSpawnPosition findClosestInChunk(@NotNull ChunkPosition chunk, @NotNull PortalSpawningContext context) {
      if (!this.generationChecker.isChunkGenerated(chunk)) {
         return null;
      } else {
         int frameSize = context.getSize().getBlockY() + 2;
         Collection<Location> obsidianBlocks = this.searchForObsidianBlocks(chunk, frameSize, context.getWorldLink());
         PortalSpawnPosition closestPosition = null;
         double closestDistance = Double.POSITIVE_INFINITY;

         for (Location block : obsidianBlocks) {
            for (int yOffset = -frameSize; yOffset <= frameSize; yOffset++) {
               for (Vector offset : XZ_CHECK_OFFSETS) {
                  Location offsetBlock = block.clone().add(offset);
                  offsetBlock.setY(offsetBlock.getY() + yOffset);
                  double distance = offsetBlock.distance(context.getPreferredLocation());
                  if (!(distance >= closestDistance)) {
                     for (PortalDirection direction : CHECKED_DIRECTIONS) {
                        if (this.validPortalExists(offsetBlock, direction, context.getSize())) {
                           closestPosition = new PortalSpawnPosition(offsetBlock, context.getSize(), direction);
                           closestDistance = distance;
                        }
                     }
                  }
               }
            }
         }

         return closestPosition;
      }
   }

   private Collection<Location> searchForObsidianBlocks(ChunkPosition chunkPos, int yIncrement, WorldLink worldLink) {
      Collection<Location> result = new ArrayList<>();
      Chunk chunk = chunkPos.getChunk();
      ChunkSnapshot snapshot = chunk.getChunkSnapshot();
      World world = chunk.getWorld();
      int chunkBlockX = chunkPos.x << 4;
      int chunkBlockZ = chunkPos.z << 4;
      int y = worldLink.getMinSpawnY();

      while (y < worldLink.getMaxSpawnY()) {
         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               if (snapshot.getBlockType(x, y, z) == Material.OBSIDIAN) {
                  result.add(new Location(world, chunkBlockX + x, y, chunkBlockZ + z));
               }
            }
         }

         y += yIncrement;
      }

      return result;
   }

   private boolean validPortalExists(Location location, PortalDirection direction, Vector size) {
      size = new Vector(size.getX() + 1.0, size.getY() + 1.0, 0.0);
      int blocks = 0;
      int validBlocks = 0;

      for (int x = 0; x <= size.getX(); x++) {
         for (int y = 0; y <= size.getY(); y++) {
            if ((x != 0 || y != 0) && (x != size.getX() || y != 0) && (x != 0 || y != size.getY()) && (x != size.getX() || y != size.getY())) {
               boolean isFrame = x == 0 || y == 0 || x == size.getX() || y == size.getY();
               Vector offset = direction.swapVector(new Vector(x, y, 0.0));
               Location blockPos = location.clone().add(offset);
               blocks++;
               Material type = blockPos.getBlock().getType();
               if (isFrame) {
                  if (type == Material.OBSIDIAN) {
                     validBlocks++;
                  }
               } else if (type == Material.AIR || type == MaterialUtil.PORTAL_MATERIAL) {
                  validBlocks++;
               }
            }
         }
      }

      double percentageValid = (double)validBlocks / blocks;
      boolean isValid = percentageValid >= VALIDITY_THRESHOLD;
      boolean isFarEnoughSpaced = this.portalManager.findClosestPortal(location, this.spawnConfig.getMinimumPortalSpawnDistance()) == null;
      boolean isInsideWorldBorder = location.getWorld().getWorldBorder().isInside(location);
      return isInsideWorldBorder && isFarEnoughSpaced && isValid;
   }
}
