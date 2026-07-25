package org.envel.immersiveportalspaperized.proxy;

import java.net.InetSocketAddress;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface IProxy {
   String getPluginVersion();

   @Deprecated
   @Nullable
   String findServer(InetSocketAddress var1);

   boolean serverExists(String var1);

   boolean playerExists(UUID var1);

   void changePlayerServer(UUID var1, String var2);
}
