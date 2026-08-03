package org.envel.immersiveportalspaperized.bukkit.net.requests;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

/**
 * CheckDestinationValidityRequest.
 */
@Getter
@Setter
public class CheckDestinationValidityRequest extends Request {
   private static final long serialVersionUID = 1L;
   private String destinationWorldName;
   private UUID destinationWorldId;
   private String originGameVersion;
}


