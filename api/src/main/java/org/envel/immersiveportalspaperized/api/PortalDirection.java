package org.envel.immersiveportalspaperized.api;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the cardinal direction and orientation of a portal.
 * <p>
 * Each direction defines a forward vector and an inversion rotation axis used
 * when transforming coordinates between the origin and destination sides.
 * </p>
 */
public enum PortalDirection {
   UP(new Vector(0.0, 1.0, 0.0), new Vector(1.0, 0.0, 0.0)),
   DOWN(new Vector(0.0, -1.0, 0.0), new Vector(1.0, 0.0, 0.0)),
   NORTH(new Vector(0.0, 0.0, 1.0), new Vector(0.0, 1.0, 0.0)),
   SOUTH(new Vector(0.0, 0.0, -1.0), new Vector(0.0, 1.0, 0.0)),
   EAST(new Vector(1.0, 0.0, 0.0), new Vector(0.0, 1.0, 0.0)),
   WEST(new Vector(-1.0, 0.0, 0.0), new Vector(0.0, 1.0, 0.0));

   private final Vector direction;
   private final Vector inversionRotationAxis;

   PortalDirection(Vector direction, Vector inversionRotationAxis) {
      this.direction = direction;
      this.inversionRotationAxis = inversionRotationAxis;
   }

   public static PortalDirection fromStorage(String string) {
      return switch (string) {
         case "EAST_WEST" -> NORTH;
         case "NORTH_SOUTH" -> EAST;
         default -> valueOf(string);
      };
   }

   public Vector toVector() {
      return this.direction;
   }

   public Vector getInversionRotationAxis() {
      return this.inversionRotationAxis;
   }

   @NotNull
   public Vector swapVector(@NotNull Vector vec) {
      return switch (this) {
         case UP, DOWN -> new Vector(vec.getX(), vec.getZ(), vec.getY());
         case NORTH, SOUTH -> vec.clone();
         case EAST, WEST -> new Vector(vec.getZ(), vec.getY(), vec.getX());
      };
   }

   @NotNull
   public IntVector swapVector(@NotNull IntVector vec) {
      return switch (this) {
         case UP, DOWN -> new IntVector(vec.getX(), vec.getZ(), vec.getY());
         case NORTH, SOUTH -> vec.clone();
         case EAST, WEST -> new IntVector(vec.getZ(), vec.getY(), vec.getX());
      };
   }

   public Location swapLocation(@NotNull Location loc) {
      return this.swapVector(loc.toVector()).toLocation(loc.getWorld());
   }

   public boolean isHorizontal() {
      return this == UP || this == DOWN;
   }

   @NotNull
   public PortalDirection getOpposite() {
      return switch (this) {
         case UP -> DOWN;
         case DOWN -> UP;
         case NORTH -> SOUTH;
         case SOUTH -> NORTH;
         case EAST -> WEST;
         case WEST -> EAST;
      };
   }
}
