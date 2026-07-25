package org.envel.immersiveportalspaperized.bukkit.net;

public interface IClientReconnectHandler {
   void prematureReconnect();

   void onClientDisconnect();

   void stop();
}
