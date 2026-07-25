package org.envel.immersiveportalspaperized.bukkit.portal.selection;

import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.command.framework.CommandException;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetSelectionRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ISelectionManager {
   @NotNull
   IPortalSelection getCurrentlySelecting();

   @Nullable
   IPortalSelection getOriginSelection();

   @Nullable
   IPortalSelection getDestSelection();

   @Nullable
   GetSelectionRequest.ExternalSelectionInfo getExternalSelection();

   void setExternalSelection(@Nullable GetSelectionRequest.ExternalSelectionInfo externalSelection);

   void trySelectOrigin() throws CommandException;

   void trySelectDestination() throws CommandException;

   void tryCreateFromSelection(Player player, boolean twoWay, boolean invert) throws CommandException;

   void tryCreateFromExternalSelection(Player player, boolean invert) throws CommandException;

   long getLastActivityTime();

   void recordActivity();
}
