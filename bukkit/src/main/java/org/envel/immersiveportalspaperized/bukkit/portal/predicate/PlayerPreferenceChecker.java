package org.envel.immersiveportalspaperized.bukkit.portal.predicate;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerData;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import org.jetbrains.annotations.NotNull;

/**
 * PlayerPreferenceChecker.
 */
public class PlayerPreferenceChecker implements PortalPredicate {
   private final IPlayerDataManager playerDataManager;
   private final String preference;

   public PlayerPreferenceChecker(IPlayerDataManager playerDataManager, String preference) {
      this.preference = preference;
      this.playerDataManager = playerDataManager;
   }

   @Override
   public boolean test(@NotNull ImmersivePortal portal, @NotNull Player player) {
      IPlayerData playerData = this.playerDataManager.getPlayerData(player);
      if (playerData == null) {
         return false;
      }

      YamlConfiguration permanentData = playerData.getPermanentData();
      return permanentData.getBoolean(this.preference);
   }
}


