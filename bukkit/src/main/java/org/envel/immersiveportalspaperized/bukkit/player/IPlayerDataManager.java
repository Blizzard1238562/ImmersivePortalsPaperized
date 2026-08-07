package org.envel.immersiveportalspaperized.bukkit.player;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetSelectionRequest;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.IPortalSelection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.net.requests.TeleportRequest;

/**
 * IPlayerDataManager.
 */
public interface IPlayerDataManager {
   @NotNull
   Collection<IPlayerData> getPlayers();

   @Nullable
   IPlayerData getPlayerData(@NotNull Player player);

   @Nullable
   default IPlayerData getPlayerData(@NotNull UUID uniqueId) {
      Player player = Bukkit.getPlayer(uniqueId);
      return player == null ? null : this.getPlayerData(player);
   }

   void onPluginDisable();

   void onPluginReload();

   void setTeleportOnJoin(TeleportRequest request);

   void setExternalSelectionOnLogin(UUID uniqueId, GetSelectionRequest.ExternalSelectionInfo selection);

   @Nullable
   IPortalSelection getDestinationSelectionWhenLoggedOut(UUID uniqueId);
}


