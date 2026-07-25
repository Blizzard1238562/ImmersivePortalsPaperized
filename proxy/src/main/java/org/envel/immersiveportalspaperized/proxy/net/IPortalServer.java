package org.envel.immersiveportalspaperized.proxy.net;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPortalServer {
   void startUp();

   void shutDown();

   void registerServer(@NotNull IClientHandler var1, @NotNull String var2);

   void onServerDisconnect(@NotNull IClientHandler var1);

   @Nullable
   IClientHandler getServer(@NotNull String var1);
}
