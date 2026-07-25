package org.envel.immersiveportalspaperized.api;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public final class IntVector implements Cloneable, Serializable {
   private static final long serialVersionUID = 1L;
   @Getter
   @Setter
   private int x;
   @Getter
   @Setter
   private int y;
   @Getter
   @Setter
   private int z;

   public IntVector(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public IntVector(double x, double y, double z) {
      this((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
   }

   public IntVector(@NotNull Location location) {
      this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
   }

   public IntVector(@NotNull Vector vector) {
      this(vector.getX(), (double)vector.getBlockY(), (double)vector.getBlockZ());
   }

   @NotNull
   public BlockVector toVector() {
      return new BlockVector(this.x, this.y, this.z);
   }

   @NotNull
   public Vector getCenterPos() {
      return new Vector(this.x + 0.5, this.y + 0.5, this.z + 0.5);
   }

   @NotNull
   public IntVector add(@NotNull IntVector other) {
      return new IntVector(this.x + other.x, this.y + other.y, this.z + other.z);
   }

   @NotNull
   public IntVector add(int x, int y, int z) {
      return new IntVector(this.x + x, this.y + y, this.z + z);
   }

   @NotNull
   public IntVector subtract(@NotNull IntVector other) {
      return new IntVector(this.x - other.x, this.y - other.y, this.z - other.z);
   }

   @NotNull
   public IntVector subtract(int x, int y, int z) {
      return new IntVector(this.x - x, this.y - y, this.z - z);
   }

   @NotNull
   public Block getBlock(@NotNull World world) {
      return world.getBlockAt(this.x, this.y, this.z);
   }

   @NotNull
   public Location toLocation(@NotNull World world) {
      return new Location(world, this.x, this.y, this.z);
   }

   @Override
   public boolean equals(Object other) {
      return !(other instanceof IntVector otherVector) ? false : otherVector.x == this.x && otherVector.y == this.y && otherVector.z == this.z;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + this.x;
      result = 31 * result + this.y;
      return 31 * result + this.z;
   }

   @NotNull
   public IntVector clone() {
      try {
         return (IntVector)super.clone();
      } catch (CloneNotSupportedException var2) {
         throw new Error(var2);
      }
   }

   @Override
   public String toString() {
      return String.format("(%d, %d, %d)", this.x, this.y, this.z);
   }
}
