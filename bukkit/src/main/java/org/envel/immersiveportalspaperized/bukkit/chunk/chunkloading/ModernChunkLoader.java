package org.envel.immersiveportalspaperized.bukkit.chunk.chunkloading;

import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Chunk;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkpos.ChunkPosition;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ModernChunkLoader implements IChunkLoader {
   private final Set<ChunkPosition> loadedChunks = ConcurrentHashMap.newKeySet();

   @Override
   public void setForceLoaded(Chunk chunk) {
      SchedulerUtil.runTask(() -> {
         chunk.setForceLoaded(true);
         this.loadedChunks.add(new ChunkPosition(chunk));
      });
   }

   @Override
   public void setForceLoaded(ChunkPosition chunk) {
      SchedulerUtil.runTask(() -> {
         chunk.getWorld().setChunkForceLoaded(chunk.x, chunk.z, true);
         this.loadedChunks.add(chunk);
      });
   }

   @Override
   public void setNotForceLoaded(@NotNull ChunkPosition chunk) {
      if (this.loadedChunks.remove(chunk)) {
         SchedulerUtil.runTask(() -> chunk.getWorld().setChunkForceLoaded(chunk.x, chunk.z, false));
      }
   }

   @Override
   public boolean isForceLoaded(@NotNull ChunkPosition chunk) {
      return this.loadedChunks.contains(chunk);
   }
}
