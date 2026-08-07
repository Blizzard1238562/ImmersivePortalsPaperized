package org.envel.immersiveportalspaperized.bukkit.net;

/**
 * IClientReconnectHandler.
 */
public interface IClientReconnectHandler {
   void prematureReconnect();

   void onClientDisconnect();

   void stop();
}


