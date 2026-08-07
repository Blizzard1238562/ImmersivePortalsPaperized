package org.envel.immersiveportalspaperized.proxy.net;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lifecycle and registry for connected Bukkit server handlers.
 */
public interface IPortalServer {
   void startUp();

   void shutDown();

   void registerServer(@NotNull IClientHandler serverHandler, @NotNull String serverName);

   void onServerDisconnect(@NotNull IClientHandler handler);

   @Nullable
   IClientHandler getServer(@NotNull String name);
}
