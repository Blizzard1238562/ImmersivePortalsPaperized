package org.envel.immersiveportalspaperized.api;

import java.util.UUID;
import lombok.Setter;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ImmersivePortalsPaperizedAPI {
   @Setter
   private static ImmersivePortalsPaperizedAPI instance = null;

   @NotNull
   public static ImmersivePortalsPaperizedAPI get() {
      if (instance == null) {
         throw new IllegalStateException("Attempted to call API when ImmersivePortalsPaperized was not enabled");
      } else {
         return instance;
      }
   }

   @NotNull
   public abstract ImmersivePortal createPortal(
      @NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size, @Nullable UUID owner, @Nullable String name, boolean custom
   );

   @NotNull
   public ImmersivePortal createPortal(
      @NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size, @Nullable UUID owner, @Nullable String name
   ) {
      return this.createPortal(originPosition, destinationPosition, size, owner, name, true);
   }

   @NotNull
   public ImmersivePortal createPortal(@NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size) {
      return this.createPortal(originPosition, destinationPosition, size, null, null);
   }

   public abstract void addPortalActivationPredicate(@NotNull PortalPredicate predicate);

   public abstract void removePortalActivationPredicate(@NotNull PortalPredicate predicate);

   public abstract void addPortalViewPredicate(@NotNull PortalPredicate predicate);

   public abstract void removePortalViewPredicate(@NotNull PortalPredicate predicate);

   public abstract void addPortalTeleportPredicate(@NotNull PortalPredicate predicate);

   public abstract void removePortalTeleportPredicate(@NotNull PortalPredicate predicate);

   @Nullable
   public abstract ImmersivePortal getPortalById(@NotNull UUID id);
}
