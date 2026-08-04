package org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

@Getter
@Setter
public class ChunkPosition implements Cloneable {
   public World world;
   public int x;
   public int z;

   public ChunkPosition(World world, int chunkX, int chunkZ) {
      this.world = world;
      this.x = chunkX;
      this.z = chunkZ;
   }

   public ChunkPosition(int chunkX, int chunkZ) {
      this.x = chunkX;
      this.z = chunkZ;
   }

   public ChunkPosition(Location location) {
      this.x = location.getBlockX() >> 4;
      this.z = location.getBlockZ() >> 4;
      this.world = location.getWorld();
   }

   public ChunkPosition(Vector position) {
      this.x = position.getBlockX() >> 4;
      this.z = position.getBlockZ() >> 4;
   }

   public ChunkPosition(Chunk chunk) {
      this.x = chunk.getX();
      this.z = chunk.getZ();
      this.world = chunk.getWorld();
   }

   public Chunk getChunk() {
      return this.world.getChunkAt(this.x, this.z);
   }

   public boolean isLoaded() {
      return this.world.isChunkLoaded(this.x, this.z);
   }

   public Location getBottomLeft() {
      return new Location(this.world, this.x * 16, 0.0, this.z * 16);
   }

   public Location getCenterPos() {
      return new Location(this.world, (this.x << 4) + 8, 128.0, (this.z << 4) + 8);
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else {
         return !(o instanceof ChunkPosition chunkPosition)
            ? false
            : this.x == chunkPosition.x && this.z == chunkPosition.z && this.world == chunkPosition.world;
      }
   }

   @Override
   public String toString() {
      return String.format("x: %d, z: %d", this.x, this.z);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.x, this.z, this.world);
   }

   public ChunkPosition clone() {
      try {
         return (ChunkPosition)super.clone();
      } catch (CloneNotSupportedException var2) {
         throw new Error(var2);
      }
   }
}
