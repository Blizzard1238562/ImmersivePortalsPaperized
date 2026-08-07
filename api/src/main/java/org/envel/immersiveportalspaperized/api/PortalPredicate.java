package org.envel.immersiveportalspaperized.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A functional interface for evaluating conditions on a portal for a specific player.
 * <p>
 * Used to control whether a player can see, activate, or teleport through a portal.
 * </p>
 */
public interface PortalPredicate {
   boolean test(@NotNull ImmersivePortal portal, @NotNull Player player);
}
