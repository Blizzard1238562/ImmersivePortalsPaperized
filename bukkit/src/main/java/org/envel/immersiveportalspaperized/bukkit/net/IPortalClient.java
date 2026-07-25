package org.envel.immersiveportalspaperized.bukkit.net;

import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.net.Response;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

public interface IPortalClient {
   void connect(boolean printErrors);

   default void connect() {
      this.connect(true);
   }

   void shutDown();

   boolean canReceiveRequests();

   boolean isConnectionOpen();

   boolean getShouldReconnect();

   void sendRequestToProxy(@NotNull Request request, @NotNull Consumer<Response> onFinish);

   void sendRequestToServer(Request request, String destinationServer, Consumer<Response> onFinish);
}
