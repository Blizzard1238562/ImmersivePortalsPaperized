package org.envel.immersiveportalspaperized.bukkit.player.view;

import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;

public interface PlayerPortalViewFactory {
   IPlayerPortalView create(Player player, IPortal portal);
}
