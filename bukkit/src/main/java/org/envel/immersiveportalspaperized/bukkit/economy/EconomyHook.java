package org.envel.immersiveportalspaperized.bukkit.economy;

import org.bukkit.entity.Player;

/**
 * EconomyHook.
 */
public interface EconomyHook {
   double getBalance(Player player);

   boolean hasMoney(Player player, double amount);

   boolean charge(Player player, double amount);

   String format(double amount);
}


