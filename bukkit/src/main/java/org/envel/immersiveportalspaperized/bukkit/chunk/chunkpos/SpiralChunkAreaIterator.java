package org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos;

import java.util.Iterator;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class SpiralChunkAreaIterator implements Iterator<ChunkPosition> {
   private static final ChunkPosition[] directions = new ChunkPosition[]{
      new ChunkPosition(null, 1, 0), new ChunkPosition(null, 0, 1), new ChunkPosition(null, -1, 0), new ChunkPosition(null, 0, -1)
   };
   private final ChunkPosition currentPos;
   private final ChunkPosition low;
   private final ChunkPosition high;
   private int currentDirection = 0;
   private final ChunkPosition currentLength = new ChunkPosition(1, 1);
   private int movesLeft = 1;

   public SpiralChunkAreaIterator(@NotNull ChunkPosition low, @NotNull ChunkPosition high) {
      if (low.world != high.world) {
         throw new IllegalArgumentException("The two positions must be in the same world");
      } else {
         this.currentPos = new ChunkPosition(low.world, (low.x + high.x) / 2, (low.z + high.z) / 2);
         this.low = low;
         this.high = high;
      }
   }

   public SpiralChunkAreaIterator(Location a, Location b) {
      this(new ChunkPosition(a), new ChunkPosition(b));
   }

   @Override
   public boolean hasNext() {
      return this.currentPos.x <= this.high.x && this.currentPos.z <= this.high.z && this.currentPos.x >= this.low.x && this.currentPos.z >= this.low.z;
   }

   public ChunkPosition next() {
      ChunkPosition result = this.currentPos.clone();
      if (this.movesLeft == 0) {
         if (this.currentDirection % 2 == 0) {
            this.movesLeft = this.currentLength.z;
            this.currentLength.x++;
         } else if (this.currentDirection % 2 == 1) {
            this.movesLeft = this.currentLength.x;
            this.currentLength.z++;
         }

         this.currentDirection++;
         if (this.currentDirection == directions.length) {
            this.currentDirection = 0;
         }
      }

      this.movesLeft--;
      this.currentPos.x = this.currentPos.x + directions[this.currentDirection].x;
      this.currentPos.z = this.currentPos.z + directions[this.currentDirection].z;
      return result;
   }
}
