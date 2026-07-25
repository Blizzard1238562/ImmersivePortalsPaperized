package org.envel.immersiveportalspaperized.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PortalPredicate {
   boolean test(@NotNull ImmersivePortal portal, @NotNull Player player);
}
