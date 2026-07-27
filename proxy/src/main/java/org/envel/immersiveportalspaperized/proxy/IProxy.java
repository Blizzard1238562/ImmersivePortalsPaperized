package org.envel.immersiveportalspaperized.proxy;

import java.net.InetSocketAddress;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface IProxy {
   String getPluginVersion();

   @Deprecated
   @Nullable
   String findServer(InetSocketAddress clientAddress);

   boolean serverExists(String serverName);

   boolean playerExists(UUID playerId);

   void changePlayerServer(UUID playerId, String serverName);
}
