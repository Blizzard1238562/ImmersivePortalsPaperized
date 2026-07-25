package org.envel.immersiveportalspaperized.bukkit.player.view;

import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.player.view.block.IPlayerBlockView;
import org.envel.immersiveportalspaperized.bukkit.player.view.entity.IPlayerEntityView;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;

public interface ViewFactory {
   IPlayerBlockView createBlockView(Player player, IPortal portal);

   IPlayerEntityView createEntityView(Player player, IPortal portal);
}
