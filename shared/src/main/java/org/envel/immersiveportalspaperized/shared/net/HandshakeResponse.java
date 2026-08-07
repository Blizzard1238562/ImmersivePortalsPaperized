package org.envel.immersiveportalspaperized.shared.net;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * Proxy response to a {@link Handshake}, indicating whether the connection was accepted.
 */
@Getter
@Setter
public class HandshakeResponse implements Serializable {
   private static final long serialVersionUID = 1L;
   private HandshakeResponse.Result status;

   public static enum Result {
      SUCCESS,
      PLUGIN_VERSION_MISMATCH,
      SERVER_NOT_REGISTERED;
   }
}
