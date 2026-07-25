package org.envel.immersiveportalspaperized.bukkit.entity;

import java.util.Collection;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface IEntityFinder {
   Collection<Entity> getNearbyEntities(@Nullable Collection<Entity> existing, Location location, double xRadius, double yRadius, double zRadius);

   void getNearbyEntities(Location location, double xRadius, double yRadius, double zRadius, Consumer<Entity> consumer);
}
