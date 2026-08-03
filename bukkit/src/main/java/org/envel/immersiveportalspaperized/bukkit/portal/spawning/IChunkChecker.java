package org.envel.immersiveportalspaperized.bukkit.portal.spawning;

import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.ChunkPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * IChunkChecker.
 */
public interface IChunkChecker {
   @Nullable
   PortalSpawnPosition findClosestInChunk(@NotNull ChunkPosition chunk, @NotNull PortalSpawningContext context);
}


