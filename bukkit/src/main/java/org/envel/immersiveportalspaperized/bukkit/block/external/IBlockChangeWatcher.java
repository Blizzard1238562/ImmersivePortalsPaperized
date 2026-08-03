package org.envel.immersiveportalspaperized.bukkit.block.external;

import java.util.Map;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetBlockDataChangesRequest;
import org.jetbrains.annotations.NotNull;

/**
 * IBlockChangeWatcher.
 */
public interface IBlockChangeWatcher {
   @NotNull
   Map<IntVector, Integer> checkForChanges();

   public interface Factory {
      IBlockChangeWatcher create(GetBlockDataChangesRequest request);
   }
}


