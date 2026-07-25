package org.envel.immersiveportalspaperized.bukkit.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class EconomyManager {
   private final Logger logger;
   private EconomyHook hook = null;
   private boolean isSetup = false;

   @Inject
   public EconomyManager(Logger logger) {
      this.logger = logger;
      this.setupEconomy();
   }

   private void setupEconomy() {
      if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
         try {
            VaultEconomyHook vaultHook = new VaultEconomyHook();
            if (vaultHook.isLoaded()) {
               this.hook = vaultHook;
               this.isSetup = true;
            }
         } catch (Throwable e) {
            this.logger.warning("Failed to set up Vault economy integration: %s", e.getMessage());
         }
      }
   }

   public boolean isEconomyEnabled() {
      if (!this.isSetup) {
         this.setupEconomy();
      }

      return this.isSetup && this.hook != null;
   }

   public double getBalance(Player player) {
      return !this.isEconomyEnabled() ? 0.0 : this.hook.getBalance(player);
   }

   public boolean hasMoney(Player player, double amount) {
      return !this.isEconomyEnabled() ? true : this.hook.hasMoney(player, amount);
   }

   public boolean charge(Player player, double amount) {
      return this.isEconomyEnabled() && !(amount <= 0.0) ? this.hook.charge(player, amount) : true;
   }

   public String format(double amount) {
      return !this.isEconomyEnabled() ? String.format("$%.2f", amount) : this.hook.format(amount);
   }
}
