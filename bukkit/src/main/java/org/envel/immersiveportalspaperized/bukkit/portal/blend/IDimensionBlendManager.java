package org.envel.immersiveportalspaperized.bukkit.portal.blend;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface IDimensionBlendManager {
   void performBlend(@NotNull Location origin, @NotNull Location destination);
}
