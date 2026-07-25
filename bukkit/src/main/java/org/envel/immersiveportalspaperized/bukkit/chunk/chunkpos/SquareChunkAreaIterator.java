package org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.Location;

public class SquareChunkAreaIterator implements Iterator<ChunkPosition>, Cloneable {
   private final ChunkPosition low;
   private final ChunkPosition high;
   private final ChunkPosition currentPos;

   public SquareChunkAreaIterator(ChunkPosition low, ChunkPosition high) {
      this.low = low;
      this.high = high;
      this.currentPos = low.clone();
   }

   public SquareChunkAreaIterator(Location low, Location high) {
      this(new ChunkPosition(low), new ChunkPosition(high));
   }

   @Override
   public boolean hasNext() {
      return this.currentPos.x < this.high.x || this.currentPos.z < this.high.z;
   }

   public ChunkPosition next() {
      if (this.currentPos.x < this.high.x) {
         this.currentPos.x++;
      } else {
         if (this.currentPos.z >= this.high.z) {
            throw new NoSuchElementException();
         }

         this.currentPos.z++;
         this.currentPos.x = this.low.x;
      }

      return this.currentPos.clone();
   }

   public SquareChunkAreaIterator clone() {
      return new SquareChunkAreaIterator(this.low, this.high);
   }
}
