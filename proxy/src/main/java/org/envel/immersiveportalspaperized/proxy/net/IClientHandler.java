package org.envel.immersiveportalspaperized.proxy.net;

import java.net.Socket;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.net.Response;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

/**
 * Represents a single connected Bukkit server and its request/response channel.
 */
public interface IClientHandler {
   @Nullable
   String getGameVersion();

   @Nullable
   String getServerName();

   void shutDown();

   void sendRequest(@NotNull Request request, @NotNull Consumer<Response> onFinish);

   public interface Factory {
      IClientHandler create(Socket socket);
   }
}
