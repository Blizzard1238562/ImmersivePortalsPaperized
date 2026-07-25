package org.envel.immersiveportalspaperized.bukkit.portal;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPortalManager {
   Collection<IPortal> getAllPortals();

   Collection<IPortal> getPortalsAt(Location var1);

   @Nullable
   default IPortal getPortalAt(@NotNull Location originLoc) {
      Collection<IPortal> portals = this.getPortalsAt(originLoc);
      return portals.size() == 0 ? null : portals.iterator().next();
   }

   @Nullable
   IPortal getPortalById(@NotNull UUID var1);

   @Nullable
   IPortal findClosestPortal(@NotNull Location var1, double var2, Predicate<IPortal> var4);

   @Nullable
   default IPortal findClosestPortal(@NotNull Location position, double maximumDistance) {
      return this.findClosestPortal(position, maximumDistance, portal -> true);
   }

   @Nullable
   default IPortal findClosestPortal(@NotNull Location position, @NotNull Predicate<IPortal> predicate) {
      return this.findClosestPortal(position, Double.POSITIVE_INFINITY, predicate);
   }

   @Nullable
   default IPortal findClosestPortal(@NotNull Location position) {
      return this.findClosestPortal(position, Double.POSITIVE_INFINITY);
   }

   @NotNull
   Collection<IPortal> findActivatablePortals(@NotNull Player var1);

   void registerPortal(@NotNull IPortal var1);

   int removePortalsAt(@NotNull Location var1);

   boolean removePortal(@NotNull IPortal var1);

   boolean removePortalById(@NotNull UUID var1);

   void onReload();
}
