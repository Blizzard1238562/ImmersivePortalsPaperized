package org.envel.immersiveportalspaperized.bukkit.chunk.chunkloading;

import java.util.Iterator;
import org.bukkit.Chunk;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.ChunkPosition;
import org.jetbrains.annotations.NotNull;

/**
 * IChunkLoader.
 */
public interface IChunkLoader {
   void setForceLoaded(Chunk chunk);

   default void setForceLoaded(ChunkPosition chunk) {
      this.setForceLoaded(chunk.getChunk());
   }

   default void forceLoadAllPos(@NotNull Iterator<? extends ChunkPosition> iterator) {
      while (iterator.hasNext()) {
         this.setForceLoaded(iterator.next());
      }
   }

   default void forceLoadAll(@NotNull Iterator<? extends Chunk> iterator) {
      while (iterator.hasNext()) {
         this.setForceLoaded(iterator.next());
      }
   }

   void setNotForceLoaded(@NotNull ChunkPosition chunk);

   default void setNotForceLoaded(@NotNull Chunk chunk) {
      this.setNotForceLoaded(new ChunkPosition(chunk));
   }

   default void unForceLoadAllPos(@NotNull Iterator<? extends ChunkPosition> iterator) {
      while (iterator.hasNext()) {
         this.setNotForceLoaded(iterator.next());
      }
   }

   default void unForceLoadAll(@NotNull Iterator<? extends Chunk> iterator) {
      while (iterator.hasNext()) {
         this.setNotForceLoaded(new ChunkPosition(iterator.next()));
      }
   }

   boolean isForceLoaded(@NotNull ChunkPosition chunk);

   default boolean isForceLoaded(@NotNull Chunk chunk) {
      return this.isForceLoaded(new ChunkPosition(chunk));
   }
}


