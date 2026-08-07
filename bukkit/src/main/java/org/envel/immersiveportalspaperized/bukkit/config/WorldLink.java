package org.envel.immersiveportalspaperized.bukkit.config;

import java.util.Objects;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.envel.immersiveportalspaperized.bukkit.util.HeightUtil;
import org.jetbrains.annotations.NotNull;

/**
 * WorldLink.
 */
@Getter
public class WorldLink {
   private final String originWorldName;
   private final String destWorldName;
   private final World originWorld;
   private final World destinationWorld;
   private final int minSpawnY;
   private final int maxSpawnY;
   private final double coordinateRescalingFactor;

   public WorldLink(ConfigurationSection config) {
      this.originWorldName = Objects.requireNonNull(config.getString("originWorld"), "Missing originWorld key in world link");
      this.destWorldName = Objects.requireNonNull(config.getString("destinationWorld"), "Missing destinationWorld key in world link");
      this.originWorld = Bukkit.getWorld(this.originWorldName);
      this.destinationWorld = Bukkit.getWorld(this.destWorldName);
      this.minSpawnY = config.getInt("minSpawnY");
      this.maxSpawnY = config.getInt("maxSpawnY");
      this.coordinateRescalingFactor = config.getDouble("coordinateRescalingFactor");
   }

   public WorldLink(World originWorld, World destinationWorld, double coordinateRescalingFactor, int minSpawnY, int maxSpawnY) {
      this.originWorldName = originWorld.getName();
      this.destWorldName = destinationWorld.getName();
      this.originWorld = originWorld;
      this.destinationWorld = destinationWorld;
      this.minSpawnY = minSpawnY;
      this.maxSpawnY = maxSpawnY;
      this.coordinateRescalingFactor = coordinateRescalingFactor;
   }

   public WorldLink(World originWorld, World destinationWorld, double coordinateRescalingFactor, int yMinSpace) {
      this(
         originWorld,
         destinationWorld,
         coordinateRescalingFactor,
         HeightUtil.getMinHeight(destinationWorld) + yMinSpace,
         getMaxHeight(destinationWorld) - yMinSpace
      );
   }

   private static int getMaxHeight(World world) {
      return world.getEnvironment() == Environment.NETHER ? 128 : HeightUtil.getMaxHeight(world);
   }

   public boolean isValid() {
      return this.originWorld != null && this.destinationWorld != null;
   }

   @NotNull
   public Location moveFromOriginWorld(@NotNull Location loc) {
      if (loc.getWorld() != this.originWorld) {
         throw new IllegalArgumentException("Location's world does not match this WorldLink's origin world");
      }

      loc = loc.clone();
      loc.setX(loc.getX() * this.coordinateRescalingFactor);
      loc.setZ(loc.getZ() * this.coordinateRescalingFactor);
      loc.setWorld(this.destinationWorld);
      return loc;
   }
}


