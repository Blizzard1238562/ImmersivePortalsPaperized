package org.envel.immersiveportalspaperized.bukkit.block;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import com.google.inject.assistedinject.Assisted;

public interface IMultiBlockChangeManager {
   void addChangeOrigin(Vector position, IViewableBlockInfo newData);

   void addChangeDestination(Vector position, IViewableBlockInfo newData);

   void addChange(Vector position, WrappedBlockData newData);

   void sendChanges();

   public interface Factory {
      IMultiBlockChangeManager create(Player player, @Assisted("minChunkY") int minChunkY, @Assisted("maxChunkY") int maxChunkY);
   }
}
