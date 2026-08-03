package org.envel.immersiveportalspaperized.bukkit.entity;

import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * BukkitEntityFinder.
 */
@Singleton
public class BukkitEntityFinder implements IEntityFinder {
   @Override
   public Collection<Entity> getNearbyEntities(Collection<Entity> existing, Location location, double xRadius, double yRadius, double zRadius) {
      World world = Objects.requireNonNull(location.getWorld());
      return world.getNearbyEntities(location, xRadius, yRadius, zRadius);
   }

   @Override
   public void getNearbyEntities(Location location, double xRadius, double yRadius, double zRadius, Consumer<Entity> consumer) {
      this.getNearbyEntities(null, location, xRadius, yRadius, zRadius).forEach(consumer);
   }
}


