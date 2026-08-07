package org.envel.immersiveportalspaperized.bukkit.portal.selection;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * IPortalSelection.
 */
public interface IPortalSelection extends Cloneable {
   void setPositionA(@NotNull Location posA);

   void setPositionB(@NotNull Location posB);

   @Nullable
   PortalPosition getPortalPosition();

   @Nullable
   Vector getPortalSize();

   void invertDirection();

   boolean isValid();

   IPortalSelection clone();

   @Nullable
   Location getPosA();

   @Nullable
   Location getPosB();
}


