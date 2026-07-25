package org.envel.immersiveportalspaperized.shared.net.requests;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreviousServerPutRequest extends Request {
   private static final long serialVersionUID = 1L;
   private UUID playerId;
   private String previousServer;
}
