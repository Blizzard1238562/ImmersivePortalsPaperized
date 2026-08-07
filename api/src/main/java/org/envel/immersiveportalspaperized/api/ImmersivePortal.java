package org.envel.immersiveportalspaperized.api;

import java.util.UUID;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a portal instance managed by ImmersivePortalsPaperized.
 * <p>
 * Portals connect an origin position to a destination position and may be either vanilla
 * nether portals or custom portals created by players or plugins.
 * </p>
 */
public interface ImmersivePortal {
   /**
    * Returns the unique ID of this portal.
    * @return the portal ID
    */
   @NotNull
   UUID getId();

   /**
    * Returns the UUID of the player who created this portal, or {@code null}.
    * @return the owner ID
    */
   @Nullable
   UUID getOwnerId();

   /**
    * Returns the display name of this portal, or {@code null}.
    * @return the portal name
    */
   @Nullable
   String getName();

   /**
    * Sets the display name of this portal.
    * @param name the new name, or {@code null} to clear
    */
   void setName(@Nullable String name);

   /**
    * Returns the origin position of this portal.
    * @return the origin position
    */
   @NotNull
   PortalPosition getOriginPos();

   /**
    * Returns the destination position of this portal.
    * @return the destination position
    */
   @NotNull
   PortalPosition getDestPos();

   /**
    * Returns the size of this portal.
    * @return the portal size
    */
   @NotNull
   Vector getSize();

   /**
    * Returns whether this portal crosses server boundaries.
    * @return {@code true} if cross-server
    */
   boolean isCrossServer();

   /**
    * Returns whether this portal is a custom (non-vanilla) portal.
    * @return {@code true} if custom
    */
   boolean isCustom();

   /**
    * Returns whether this portal is a vanilla nether portal.
    * @return {@code true} if nether portal
    */
   default boolean isNetherPortal() {
      return !this.isCustom();
   }

   /**
    * Removes this portal.
    * @param removeOtherDirection whether to also remove the reverse portal
    */
   void remove(boolean removeOtherDirection);
}
