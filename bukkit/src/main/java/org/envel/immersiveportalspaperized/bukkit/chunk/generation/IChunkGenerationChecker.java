package org.envel.immersiveportalspaperized.bukkit.chunk.generation;

import org.bukkit.World;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.ChunkPosition;
import org.jetbrains.annotations.NotNull;

/**
 * IChunkGenerationChecker.
 */
public interface IChunkGenerationChecker {
   boolean isChunkGenerated(@NotNull World world, int x, int z);

   default boolean isChunkGenerated(@NotNull ChunkPosition chunk) {
      return this.isChunkGenerated(chunk.getWorld(), chunk.getX(), chunk.getZ());
   }
}


