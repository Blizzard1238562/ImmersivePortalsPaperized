package org.envel.immersiveportalspaperized.shared.net.requests;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Informs the proxy of the server a player just left, so the proxy can sync the player's destination selection.
 */
@Getter
@Setter
public class PreviousServerPutRequest extends Request {
   private static final long serialVersionUID = 1L;
   private UUID playerId;
   private String previousServer;
}
