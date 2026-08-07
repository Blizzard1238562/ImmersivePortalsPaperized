package org.envel.immersiveportalspaperized.shared.net;

/**
 * Thrown when a cross-server request targets a server name that the proxy does not know about.
 */
public class ServerNotFoundException extends RequestException {
   private static final long serialVersionUID = 1L;

   public ServerNotFoundException(String serverName) {
      super("Unable to find server with name " + serverName);
   }
}
