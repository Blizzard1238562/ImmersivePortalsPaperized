package org.envel.immersiveportalspaperized.bukkit.chunk.generation;

import jakarta.inject.Singleton;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ModernChunkGenerationChecker implements IChunkGenerationChecker {
   @Override
   public boolean isChunkGenerated(@NotNull World world, int x, int z) {
      return world.isChunkGenerated(x, z);
   }
}
