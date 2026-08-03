package org.envel.immersiveportalspaperized.bukkit.net.requests;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

/**
 * GetBlockDataChangesRequest.
 */
@Getter
@Setter
public class GetBlockDataChangesRequest extends Request {
   private static final long serialVersionUID = 1L;
   private UUID changeSetId;
   private IntVector position;
   private Matrix rotateOriginToDest;
   private UUID worldId;
   private String worldName;
   private int xAndZRadius;
   private int yRadius;
}


