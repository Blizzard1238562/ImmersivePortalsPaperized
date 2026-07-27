package org.envel.immersiveportalspaperized.proxy.net;

import java.util.UUID;
import java.util.function.Consumer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.proxy.IProxy;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.IRequestHandler;
import org.envel.immersiveportalspaperized.shared.net.RequestException;
import org.envel.immersiveportalspaperized.shared.net.Response;
import org.envel.immersiveportalspaperized.shared.net.ServerNotFoundException;
import org.envel.immersiveportalspaperized.shared.net.requests.RelayRequest;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;
import org.envel.immersiveportalspaperized.shared.net.requests.TeleportRequest;

@Singleton
public class ProxyRequestHandler implements IRequestHandler {
   private final IPortalServer portalServer;
   private final Logger logger;
   private final IProxy proxy;

   @Inject
   public ProxyRequestHandler(IPortalServer portalServer, Logger logger, IProxy proxy) {
      this.portalServer = portalServer;
      this.logger = logger;
      this.proxy = proxy;
   }

   @Override
   public void handleRequest(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
      this.logger.finer("Processing request of type: %s", request.getClass().getName());

      try {
         if (request instanceof RelayRequest) {
            this.handleRelayRequest((RelayRequest)request, onFinish);
         } else {
            if (!(request instanceof TeleportRequest)) {
               throw new IllegalStateException("Unknown request type " + request.getClass().getName());
            }

            this.handleTeleportRequest((TeleportRequest)request, onFinish);
         }
      } catch (RequestException e) {
         Response response = new Response();
         response.setError(e);
         onFinish.accept(response);
      } catch (Exception e) {
         Response response = new Response();
         response.setError(new RequestException(e, "Internal error occurred on the proxy while processing request"));
         onFinish.accept(response);
      }
   }

   private IClientHandler checkExists(String serverName) throws ServerNotFoundException {
      IClientHandler clientHandler = this.portalServer.getServer(serverName);
      if (clientHandler == null) {
         throw new ServerNotFoundException(serverName);
      } else {
         return clientHandler;
      }
   }

   private void handleRelayRequest(RelayRequest request, Consumer<Response> onFinish) throws RequestException {
      IClientHandler clientHandler = this.checkExists(request.getDestination());
      clientHandler.sendRequest(request, onFinish);
   }

   private void handleTeleportRequest(TeleportRequest request, Consumer<Response> onFinish) throws RequestException {
      IClientHandler clientHandler = this.checkExists(request.getDestServer());
      this.logger.fine("Requesting teleport on join for player %s", request.getPlayerId());
      clientHandler.sendRequest(request, response -> {
         try {
            response.checkForErrors();
            this.logger.fine("No errors while setting to teleport on join, moving server!");
            UUID playerId = request.getPlayerId();
            if (!this.proxy.playerExists(playerId)) {
               throw new RequestException(String.format("No player with UUID %s exists", playerId));
            }

            this.proxy.changePlayerServer(playerId, clientHandler.getServerName());
            onFinish.accept(new Response());
         } catch (RequestException e) {
            Response errorResponse = new Response();
            errorResponse.setError(e);
            onFinish.accept(errorResponse);
         }
      });
   }
}
