package org.envel.immersiveportalspaperized.bukkit.block.fetch;

import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.jetbrains.annotations.NotNull;

/**
 * IBlockDataFetcher.
 */
public interface IBlockDataFetcher {
   void update();

   boolean isReady();

   @NotNull
   BlockData getData(@NotNull IntVector position);
}


