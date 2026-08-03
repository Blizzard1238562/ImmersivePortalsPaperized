package org.envel.immersiveportalspaperized.bukkit.portal;

import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.block.IBlockMap;
import org.envel.immersiveportalspaperized.bukkit.entity.IPortalEntityManager;
import org.envel.immersiveportalspaperized.bukkit.math.PortalTransformations;
import com.google.inject.assistedinject.Assisted;
import org.jetbrains.annotations.NotNull;

/**
 * IPortal.
 */
public interface IPortal extends ImmersivePortal {
   void onUpdate();

   void onViewUpdate();

   void onActivate();

   void onDeactivate();

   void onViewActivate();

   void onViewDeactivate();

   @NotNull
   PortalTransformations getTransformations();

   @NotNull
   IBlockMap getViewableBlocks();

   @NotNull
   IPortalEntityManager getEntityList();

   String getPermissionPath();

   boolean isRegistered();

   boolean allowsNonPlayerTeleportation();

   void setAllowsNonPlayerTeleportation(boolean allowNonPlayerTeleportation);

   double getPrice();

   void setPrice(double price);

   @Nullable
   String getEffectPreset();

   void setEffectPreset(@Nullable String effectPreset);

   boolean isSoundEnabled();

   void setSoundEnabled(boolean soundEnabled);

   public interface Factory {
      IPortal create(
         @Assisted("originPos") @NotNull PortalPosition originPos,
         @Assisted("destPos") @NotNull PortalPosition destPos,
         Vector size,
         @Assisted("isCustom") boolean isCustom,
         @Assisted("id") UUID id,
         @Nullable @Assisted("ownerId") UUID ownerId,
         @Nullable @Assisted("name") String name,
         @Assisted("allowNonPlayerTeleportation") boolean allowNonPlayerTeleportation
      );
   }
}


