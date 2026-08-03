package org.envel.immersiveportalspaperized.api;

import java.util.UUID;
import lombok.Setter;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public API entry point for ImmersivePortalsPaperized.
 * <p>
 * Provides methods to create portals and register custom predicates that control
 * portal activation, viewing, and teleportation. Obtain the API instance via
 * {@link #get()} after the plugin has enabled successfully.
 * </p>
 */
public abstract class ImmersivePortalsPaperizedAPI {
   @Setter
   private static ImmersivePortalsPaperizedAPI instance = null;

   /**
    * Returns the active API instance.
    * @return the API instance
    * @throws IllegalStateException if the plugin is not enabled
    */
   @NotNull
   public static ImmersivePortalsPaperizedAPI get() {
      if (instance == null) {
         throw new IllegalStateException("Attempted to call API when ImmersivePortalsPaperized was not enabled");
      } else {
         return instance;
      }
   }

   /**
    * Creates a new portal between two positions.
    * @param originPosition the origin position
    * @param destinationPosition the destination position
    * @param size the size of the portal
    * @param owner the owner UUID, or {@code null}
    * @param name the portal name, or {@code null}
    * @param custom whether the portal is custom
    * @return the created portal
    */
   @NotNull
   public abstract ImmersivePortal createPortal(
      @NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size, @Nullable UUID owner, @Nullable String name, boolean custom
   );

   /**
    * Creates a new portal with default custom flag.
    */
   @NotNull
   public ImmersivePortal createPortal(
      @NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size, @Nullable UUID owner, @Nullable String name
   ) {
      return this.createPortal(originPosition, destinationPosition, size, owner, name, true);
   }

   /**
    * Creates a new portal with minimal parameters.
    */
   @NotNull
   public ImmersivePortal createPortal(@NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size) {
      return this.createPortal(originPosition, destinationPosition, size, null, null);
   }

   /**
    * Registers a predicate that controls whether a portal can be activated.
    * @param predicate the predicate to add
    */
   public abstract void addPortalActivationPredicate(@NotNull PortalPredicate predicate);

   /**
    * Removes a previously registered activation predicate.
    * @param predicate the predicate to remove
    * @throws UnknownPredicateException if the predicate was not registered
    */
   public abstract void removePortalActivationPredicate(@NotNull PortalPredicate predicate);

   /**
    * Registers a predicate that controls whether a portal is visible to a player.
    * @param predicate the predicate to add
    */
   public abstract void addPortalViewPredicate(@NotNull PortalPredicate predicate);

   /**
    * Removes a previously registered view predicate.
    * @param predicate the predicate to remove
    * @throws UnknownPredicateException if the predicate was not registered
    */
   public abstract void removePortalViewPredicate(@NotNull PortalPredicate predicate);

   /**
    * Registers a predicate that controls whether a player can teleport through a portal.
    * @param predicate the predicate to add
    */
   public abstract void addPortalTeleportPredicate(@NotNull PortalPredicate predicate);

   /**
    * Removes a previously registered teleport predicate.
    * @param predicate the predicate to remove
    * @throws UnknownPredicateException if the predicate was not registered
    */
   public abstract void removePortalTeleportPredicate(@NotNull PortalPredicate predicate);

   /**
    * Looks up a portal by its unique ID.
    * @param id the portal ID
    * @return the portal, or {@code null} if not found
    */
   @Nullable
   public abstract ImmersivePortal getPortalById(@NotNull UUID id);
}
