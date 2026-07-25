package org.envel.immersiveportalspaperized.bukkit.player;

import java.util.Collection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.ISelectionManager;
import org.jetbrains.annotations.NotNull;

public interface IPlayerData {
   @NotNull
   Collection<IPortal> getViewedPortals();

   @NotNull
   Player getPlayer();

   @NotNull
   YamlConfiguration getPermanentData();

   void savePermanentData();

   void freezePortalViews();

   @NotNull
   ISelectionManager getSelection();

   void setSelection(@NotNull ISelectionManager selection);

   void onUpdate(boolean skipRendering);

   default void onUpdate() {
      this.onUpdate(false);
   }

   void onPluginDisable();

   void onLogout();

   public interface Factory {
      IPlayerData create(Player player);
   }
}
