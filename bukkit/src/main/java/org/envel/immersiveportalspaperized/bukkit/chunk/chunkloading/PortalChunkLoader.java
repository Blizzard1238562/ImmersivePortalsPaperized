package org.envel.immersiveportalspaperized.bukkit.chunk.chunkloading;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.SquareChunkAreaIterator;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PortalChunkLoader implements IPortalChunkLoader {
   private final RenderConfig config;
   private final IChunkLoader chunkLoader;

   @Inject
   public PortalChunkLoader(RenderConfig config, IChunkLoader chunkLoader) {
      this.config = config;
      this.chunkLoader = chunkLoader;
   }

   private SquareChunkAreaIterator getAreaIterator(PortalPosition destPosition) {
      Location destLoc = destPosition.getLocation();
      Vector radiusFromPortal = this.config.getHalfFullSize().toVector();
      return new SquareChunkAreaIterator(destLoc.clone().subtract(radiusFromPortal), destLoc.clone().add(radiusFromPortal));
   }

   @Override
   public void forceloadPortalChunks(@NotNull PortalPosition destPosition) {
      if (!destPosition.isExternal()) {
         this.chunkLoader.forceLoadAllPos(this.getAreaIterator(destPosition));
      }
   }

   @Override
   public void unforceloadPortalChunks(@NotNull PortalPosition destPosition) {
      if (!destPosition.isExternal()) {
         this.chunkLoader.unForceLoadAllPos(this.getAreaIterator(destPosition));
      }
   }
}
