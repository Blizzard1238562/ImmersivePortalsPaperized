package org.envel.immersiveportalspaperized.shared.net;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * Initial handshake payload sent from a Bukkit server to the proxy on connection.
 */
@Getter
@Setter
public class Handshake implements Serializable {
   private static final long serialVersionUID = 1L;
   private String pluginVersion;
   private String gameVersion;
   private int serverPort;
   private String overrideServerName;
}
