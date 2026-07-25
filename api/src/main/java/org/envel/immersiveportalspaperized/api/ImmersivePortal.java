package org.envel.immersiveportalspaperized.api;

import java.util.UUID;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ImmersivePortal {
   @NotNull
   UUID getId();

   @Nullable
   UUID getOwnerId();

   @Nullable
   String getName();

   void setName(@Nullable String name);

   @NotNull
   PortalPosition getOriginPos();

   @NotNull
   PortalPosition getDestPos();

   @NotNull
   Vector getSize();

   boolean isCrossServer();

   boolean isCustom();

   default boolean isNetherPortal() {
      return !this.isCustom();
   }

   void remove(boolean removeOtherDirection);
}
