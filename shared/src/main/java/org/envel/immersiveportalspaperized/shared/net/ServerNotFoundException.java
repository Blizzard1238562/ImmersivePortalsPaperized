package org.envel.immersiveportalspaperized.shared.net;

public class ServerNotFoundException extends RequestException {
   private static final long serialVersionUID = 1L;

   public ServerNotFoundException(String serverName) {
      super("Unable to find server with name " + serverName);
   }
}
