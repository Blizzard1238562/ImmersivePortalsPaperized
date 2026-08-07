package org.envel.immersiveportalspaperized.proxy;

import java.net.InetSocketAddress;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Abstraction over the proxy platform, used to look up servers/players and move players across servers.
 */
public interface IProxy {
   String getPluginVersion();

   @Deprecated
   @Nullable
   String findServer(InetSocketAddress clientAddress);

   boolean serverExists(String serverName);

   boolean playerExists(UUID playerId);

   void changePlayerServer(UUID playerId, String serverName);
}
