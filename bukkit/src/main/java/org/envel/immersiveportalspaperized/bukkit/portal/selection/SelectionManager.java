package org.envel.immersiveportalspaperized.bukkit.portal.selection;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.command.framework.CommandException;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetSelectionRequest;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import com.google.inject.Inject;

/**
 * SelectionManager.
 */
public class SelectionManager implements ISelectionManager {
   private final MessageConfig messageConfig;
   @Getter
   private final IPortalSelection currentlySelecting;
   private final IPortal.Factory portalFactory;
   private final IPortalManager portalManager;
   @Getter
   private IPortalSelection originSelection;
   @Getter
   private IPortalSelection destSelection;
   @Getter
   @Setter
   private GetSelectionRequest.ExternalSelectionInfo externalSelection;
   private long lastActivityTime = System.currentTimeMillis();

   @Inject
   public SelectionManager(MessageConfig messageConfig, IPortalSelection currentlySelecting, IPortal.Factory portalFactory, IPortalManager portalManager) {
      this.messageConfig = messageConfig;
      this.currentlySelecting = currentlySelecting;
      this.portalFactory = portalFactory;
      this.portalManager = portalManager;
   }

   @Override
   public void trySelectOrigin() throws CommandException {
      this.verifyCurrentSelection();
      this.originSelection = this.currentlySelecting.clone();
   }

   @Override
   public void trySelectDestination() throws CommandException {
      this.verifyCurrentSelection();
      this.destSelection = this.currentlySelecting.clone();
   }

   private void verifyCurrentSelection() throws CommandException {
      if (!this.currentlySelecting.isValid()) {
         throw new CommandException(this.messageConfig.getErrorMessage("invalidSelection"));
      }
   }

   @Override
   public void tryCreateFromSelection(Player player, boolean twoWay, boolean invert) throws CommandException {
      if (this.originSelection != null && this.destSelection != null) {
         if (!this.originSelection.getPortalSize().equals(this.destSelection.getPortalSize())) {
            throw new CommandException(this.messageConfig.getErrorMessage("differentSizes"));
         } else {
            if (invert) {
               this.destSelection.invertDirection();
            }

            IPortal portal = this.portalFactory
               .create(
                  this.originSelection.getPortalPosition(),
                  this.destSelection.getPortalPosition(),
                  this.originSelection.getPortalSize(),
                  true,
                  UUID.randomUUID(),
                  player.getUniqueId(),
                  null,
                  true
               );
            this.portalManager.registerPortal(portal);
            if (twoWay) {
               IPortal reversePortal = this.portalFactory
                  .create(
                     this.destSelection.getPortalPosition(),
                     this.originSelection.getPortalPosition(),
                     this.originSelection.getPortalSize(),
                     true,
                     UUID.randomUUID(),
                     player.getUniqueId(),
                     null,
                     true
                  );
               this.portalManager.registerPortal(reversePortal);
            }

            this.originSelection = null;
            this.destSelection = null;
         }
      } else {
         throw new CommandException(this.messageConfig.getErrorMessage("mustSelectBothSides"));
      }
   }

   @Override
   public void tryCreateFromExternalSelection(Player player, boolean invert) throws CommandException {
      if (this.originSelection != null && this.externalSelection != null) {
         Vector externalSize = new Vector(this.externalSelection.getSizeX(), this.externalSelection.getSizeY(), 0.0);
         if (invert) {
            this.originSelection.invertDirection();
         }

         if (!this.originSelection.getPortalSize().equals(externalSize)) {
            throw new CommandException(this.messageConfig.getErrorMessage("differentSizes"));
         } else {
            IPortal portal = this.portalFactory
               .create(
                  this.originSelection.getPortalPosition(),
                  this.externalSelection.getPosition(),
                  this.originSelection.getPortalSize(),
                  true,
                  UUID.randomUUID(),
                  player.getUniqueId(),
                  null,
                  true
               );
            this.portalManager.registerPortal(portal);
         }
      } else {
         throw new CommandException(this.messageConfig.getErrorMessage("mustSelectBothSides"));
      }
   }

   @Override
   public long getLastActivityTime() {
      return this.lastActivityTime;
   }

   @Override
   public void recordActivity() {
      this.lastActivityTime = System.currentTimeMillis();
   }
}


