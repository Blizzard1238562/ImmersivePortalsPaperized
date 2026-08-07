package org.envel.immersiveportalspaperized.bukkit.chunk.chunkloading;

import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.jetbrains.annotations.NotNull;

/**
 * IPortalChunkLoader.
 */
public interface IPortalChunkLoader {
   void forceloadPortalChunks(@NotNull PortalPosition destPosition);

   void unforceloadPortalChunks(@NotNull PortalPosition destPosition);
}


