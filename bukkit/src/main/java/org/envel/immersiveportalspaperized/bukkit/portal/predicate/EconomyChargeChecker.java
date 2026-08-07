package org.envel.immersiveportalspaperized.bukkit.portal.predicate;

import io.foxserver.common.locale.LocaleAPI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.economy.EconomyManager;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;

/**
 * EconomyChargeChecker.
 */
public class EconomyChargeChecker implements PortalPredicate {
   private static final long MESSAGE_COOLDOWN_MS = 3000L;
   private final EconomyManager economyManager;
   private final LocaleAPI localeApi;
   private final MessageConfig messageConfig;
   private final Map<UUID, Long> messageCooldowns = new HashMap<>();

   @Inject
   public EconomyChargeChecker(EconomyManager economyManager, LocaleAPI localeApi, MessageConfig messageConfig) {
      this.economyManager = economyManager;
      this.localeApi = localeApi;
      this.messageConfig = messageConfig;
   }

   @Override
   public boolean test(@NotNull ImmersivePortal portal, @NotNull Player player) {
      IPortal iPortal = (IPortal)portal;
      double price = iPortal.getPrice();
      if (price <= 0.0) {
         return true;
      } else if (!this.economyManager.isEconomyEnabled()) {
         return true;
      } else if (this.economyManager.hasMoney(player, price) && this.economyManager.charge(player, price)) {
         String msg = this.localeApi.getMessage(player, "economy_charged", "{amount}", this.economyManager.format(price));
         if (msg != null) {
            player.sendMessage(this.messageConfig.formatMiniMessage(this.messageConfig.getPrefix(player) + msg));
         } else {
            player.sendMessage(this.messageConfig.formatMiniMessage("<green>Charged " + this.economyManager.format(price) + " to use this portal.</green>"));
         }

         return true;
      } else {
         long now = System.currentTimeMillis();
         UUID playerId = player.getUniqueId();
         if (now - this.messageCooldowns.getOrDefault(playerId, 0L) > MESSAGE_COOLDOWN_MS) {
            String msg = this.localeApi
               .getMessage(
                  player,
                  "economy_not_enough_money",
                  "{amount}",
                  this.economyManager.format(price),
                  "{balance}",
                  this.economyManager.format(this.economyManager.getBalance(player))
               );
            if (msg != null) {
               player.sendMessage(this.messageConfig.formatMiniMessage(this.messageConfig.getPrefix(player) + msg));
            } else {
               player.sendMessage(
                  this.messageConfig
                     .formatMiniMessage(
                        "<red>You need "
                           + this.economyManager.format(price)
                           + " to pass through this portal! (Your balance: "
                           + this.economyManager.format(this.economyManager.getBalance(player))
                           + ")</red>"
                     )
               );
            }

            this.messageCooldowns.put(playerId, now);
         }

         return false;
      }
   }
}


