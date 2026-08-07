package org.envel.immersiveportalspaperized.bukkit.player.view.block;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.block.IViewableBlockInfo;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;

/**
 * IPlayerBlockStates.
 */
public interface IPlayerBlockStates {
   void resetAndUpdate(int minChunkY, int maxChunkY);

   boolean setViewable(Vector position, IViewableBlockInfo block);

   boolean setNonViewable(Vector position, IViewableBlockInfo block);

   public interface Factory {
      IPlayerBlockStates create(Player player, IPortal portal);
   }
}


