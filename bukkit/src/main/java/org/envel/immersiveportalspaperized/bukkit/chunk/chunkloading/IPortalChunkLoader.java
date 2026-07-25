package org.envel.immersiveportalspaperized.bukkit.chunk.chunkloading;

import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.jetbrains.annotations.NotNull;

public interface IPortalChunkLoader {
   void forceloadPortalChunks(@NotNull PortalPosition destPosition);

   void unforceloadPortalChunks(@NotNull PortalPosition destPosition);
}
