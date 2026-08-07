package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.envel.immersiveportalspaperized.bukkit.math.PortalTransformations;
import org.jetbrains.annotations.NotNull;

/**
 * EntityInfo.
 */
@Getter
public class EntityInfo {
   private static final Random entityIdGenerator = new Random();
   private final Entity entity;
   private final int entityId;
   private final UUID entityUniqueId;
   private final Matrix translation;
   private final Matrix rotation;

   public EntityInfo(@NotNull PortalTransformations transformations, @NotNull Entity entity) {
      this.entity = entity;
      this.entityId = entityIdGenerator.nextInt() & Integer.MAX_VALUE;
      this.entityUniqueId = UUID.randomUUID();
      this.translation = transformations.getDestinationToOrigin();
      this.rotation = transformations.getRotateToOrigin();
   }

   public EntityInfo(@NotNull Entity entity) {
      this.entity = entity;
      this.entityId = entity.getEntityId();
      this.entityUniqueId = entity.getUniqueId();
      this.translation = Matrix.makeIdentity();
      this.rotation = Matrix.makeIdentity();
   }

   public Location findRenderedLocation() {
      Location actualPos = this.entity.getLocation();
      Location atOrigin = this.translation.transform(actualPos.toVector()).toLocation(Objects.requireNonNull(actualPos.getWorld()));
      atOrigin.setDirection(this.rotation.transform(actualPos.getDirection()));
      return atOrigin;
   }
}


