package org.envel.immersiveportalspaperized.bukkit;

import java.util.UUID;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.ImmersivePortalsPaperizedAPI;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApiPortalWrapper implements ImmersivePortal {
   private final UUID id;

   public ApiPortalWrapper(UUID id) {
      this.id = id;
   }

   private ImmersivePortal getDelegate() {
      ImmersivePortalsPaperizedAPI apiInstance = ImmersivePortalsPaperizedAPI.get();
      if (!(apiInstance instanceof API)) {
         throw new IllegalStateException("API is not fully initialized");
      } else {
         IPortalManager portalManager = ((API)apiInstance).getPortalManager();
         ImmersivePortal portal = portalManager.getPortalById(this.id);
         if (portal == null) {
            throw new IllegalStateException("Portal with ID " + this.id + " is no longer registered");
         } else {
            return portal;
         }
      }
   }

   @NotNull
   @Override
   public UUID getId() {
      return this.id;
   }

   @Nullable
   @Override
   public UUID getOwnerId() {
      return this.getDelegate().getOwnerId();
   }

   @Nullable
   @Override
   public String getName() {
      return this.getDelegate().getName();
   }

   @Override
   public void setName(@Nullable String name) {
      this.getDelegate().setName(name);
   }

   @NotNull
   @Override
   public PortalPosition getOriginPos() {
      return this.getDelegate().getOriginPos();
   }

   @NotNull
   @Override
   public PortalPosition getDestPos() {
      return this.getDelegate().getDestPos();
   }

   @NotNull
   @Override
   public Vector getSize() {
      return this.getDelegate().getSize();
   }

   @Override
   public boolean isCrossServer() {
      return this.getDelegate().isCrossServer();
   }

   @Override
   public boolean isCustom() {
      return this.getDelegate().isCustom();
   }

   @Override
   public void remove(boolean removeOtherDirection) {
      this.getDelegate().remove(removeOtherDirection);
   }
}
