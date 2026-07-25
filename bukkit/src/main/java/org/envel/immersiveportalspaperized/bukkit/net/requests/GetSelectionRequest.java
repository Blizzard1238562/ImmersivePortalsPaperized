package org.envel.immersiveportalspaperized.bukkit.net.requests;

import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.IPortalSelection;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

public class GetSelectionRequest extends Request {
   private static final long serialVersionUID = 1L;
   @Getter
   @Setter
   private UUID playerId;

   public static class ExternalSelectionInfo implements Serializable {
      @Getter
      private final PortalPosition position;
      @Getter
      private final int sizeX;
      @Getter
      private final int sizeY;

      public ExternalSelectionInfo(IPortalSelection portalSelection) {
         if (!portalSelection.isValid()) {
            throw new IllegalArgumentException("Cannot create ExternalSelectionInfo from an invalid selection");
         }

         this.position = portalSelection.getPortalPosition();
         this.sizeX = portalSelection.getPortalSize().getBlockX();
         this.sizeY = portalSelection.getPortalSize().getBlockY();
      }
   }
}
