package org.envel.immersiveportalspaperized.bukkit.portal.blend;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Map.Entry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.config.PortalSpawnConfig;
import org.envel.immersiveportalspaperized.bukkit.util.MaterialUtil;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * DimensionBlendManager.
 */
@Singleton
public class DimensionBlendManager implements IDimensionBlendManager {
   private static final double INITIAL_CHANCE = 1.0;
   private static final Material[] BLACKLISTED_COPY_BLOCKS = new Material[]{
      Material.OBSIDIAN,
      Material.BEDROCK,
      MaterialUtil.PORTAL_MATERIAL,
      Material.AIR,
      Material.BARRIER,
      Material.DIAMOND_BLOCK,
      Material.EMERALD_BLOCK,
      Material.IRON_BLOCK
   };
   private final PortalSpawnConfig spawnConfig;
   private final Random random = new Random();
   private final Logger logger;

   @Inject
   public DimensionBlendManager(PortalSpawnConfig spawnConfig, Logger logger) {
      this.spawnConfig = spawnConfig;
      this.logger = logger;
   }

   @NotNull
   private Material findFillInBlock(@NotNull Location destination) {
      return switch (Objects.requireNonNull(destination.getWorld(), "World of destination location cannot be null").getEnvironment()) {
         case NETHER -> Material.NETHERRACK;
         case NORMAL -> Material.STONE;
         case THE_END -> Material.END_STONE;
         default -> Material.AIR;
      };
   }

   @Override
   public void performBlend(@NotNull Location origin, @NotNull Location destination) {
      if (SchedulerUtil.isFolia()) {
         this.performBlendFolia(origin, destination);
      } else {
         this.logger.fine("Origin for blend: %s.", origin.toVector());
         int blockRadius = (int)(1.0 / this.spawnConfig.getBlendFallOff() + 4.0 + 1.0);
         Material fillInBlock = this.findFillInBlock(destination);

         for (int z = -blockRadius; z < blockRadius; z++) {
            for (int y = -blockRadius; y < blockRadius; y++) {
               for (int x = -blockRadius; x < blockRadius; x++) {
                  Vector relativePos = new Vector(x, y, z);
                  double swapChance = this.calculateSwapChance(relativePos);
                  if (!(this.random.nextDouble() > swapChance)) {
                     Location originPos = origin.clone().add(relativePos);
                     Location destPos = destination.clone().add(this.applyRandomOffset(relativePos, 10.0));
                     Material originType = originPos.getBlock().getType();
                     Material destType = destPos.getBlock().getType();
                     if (!destType.isSolid()) {
                        destType = fillInBlock;
                     }

                     boolean skip = false;

                     for (Material type : BLACKLISTED_COPY_BLOCKS) {
                        if (originType == type || destType == type) {
                           skip = true;
                           break;
                        }
                     }

                     if (!skip) {
                        originPos.getBlock().setType(destType);
                     }
                  }
               }
            }
         }
      }
   }

   private void performBlendFolia(@NotNull Location origin, @NotNull Location destination) {
      int blockRadius = (int)(1.0 / this.spawnConfig.getBlendFallOff() + 4.0 + 1.0);
      Material fillInBlock = this.findFillInBlock(destination);
      SchedulerUtil.runAtLocation(
         destination,
         () -> {
            Map<Vector, Material> destTypes = new HashMap<>();

            for (int z = -blockRadius; z < blockRadius; z++) {
               for (int y = -blockRadius; y < blockRadius; y++) {
                  for (int x = -blockRadius; x < blockRadius; x++) {
                     Vector relativePos = new Vector(x, y, z);
                     double swapChance = this.calculateSwapChance(relativePos);
                     if (!(this.random.nextDouble() > swapChance)) {
                        Location destPos = destination.clone().add(this.applyRandomOffset(relativePos, 10.0));
                        World destWorld = destPos.getWorld();
                        Material destType = destWorld != null
                           ? destWorld.getBlockData(destPos.getBlockX(), destPos.getBlockY(), destPos.getBlockZ()).getMaterial()
                           : Material.AIR;
                        if (!destType.isSolid()) {
                           destType = fillInBlock;
                        }

                        destTypes.put(relativePos, destType);
                     }
                  }
               }
            }

            SchedulerUtil.runAtLocation(origin, () -> {
               for (Entry<Vector, Material> entry : destTypes.entrySet()) {
                  Vector relativePosx = entry.getKey();
                  Material destTypex = entry.getValue();
                  Location originPos = origin.clone().add(relativePosx);
                  World origWorld = originPos.getWorld();
                  if (origWorld != null) {
                     Material originType = origWorld.getBlockData(originPos.getBlockX(), originPos.getBlockY(), originPos.getBlockZ()).getMaterial();
                     boolean skip = false;

                     for (Material type : BLACKLISTED_COPY_BLOCKS) {
                        if (originType == type || destTypex == type) {
                           skip = true;
                           break;
                        }
                     }

                     if (!skip) {
                        originPos.getBlock().setType(destTypex);
                     }
                  }
               }
            });
         }
      );
   }

   private Vector applyRandomOffset(Vector vec, double power) {
      Vector other = new Vector();
      other.setX(vec.getX() + (this.random.nextDouble() - 0.5) * power);
      other.setY(vec.getY() + (this.random.nextDouble() - 0.5) * power);
      other.setZ(vec.getZ() + (this.random.nextDouble() - 0.5) * power);
      return other;
   }

   private double calculateSwapChance(Vector relativePos) {
      double distance = relativePos.length();
      return INITIAL_CHANCE - distance * this.spawnConfig.getBlendFallOff();
   }
}


