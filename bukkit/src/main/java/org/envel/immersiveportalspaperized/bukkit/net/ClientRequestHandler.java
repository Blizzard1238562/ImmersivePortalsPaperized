package org.envel.immersiveportalspaperized.bukkit.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.block.external.IExternalBlockWatcherManager;
import org.envel.immersiveportalspaperized.bukkit.net.requests.CheckDestinationValidityRequest;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetBlockDataChangesRequest;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetSelectionRequest;
import org.envel.immersiveportalspaperized.bukkit.net.requests.TestForwardedRequest;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.IPortalSelection;
import org.envel.immersiveportalspaperized.bukkit.util.Pair;
import org.envel.immersiveportalspaperized.bukkit.util.VersionUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.IRequestHandler;
import org.envel.immersiveportalspaperized.shared.net.RequestException;
import org.envel.immersiveportalspaperized.shared.net.Response;
import org.envel.immersiveportalspaperized.shared.net.requests.PreviousServerPutRequest;
import org.envel.immersiveportalspaperized.shared.net.requests.RelayRequest;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;
import org.envel.immersiveportalspaperized.shared.net.requests.TeleportRequest;

@Singleton
public class ClientRequestHandler implements IRequestHandler {
   private final Logger logger;
   private final IExternalBlockWatcherManager blockWatcherManager;
   private final IPlayerDataManager playerDataManager;
   private final IPortalClient portalClient;
   private final ConcurrentLinkedQueue<Pair<Request, Consumer<Response>>> awaitingHandling = new ConcurrentLinkedQueue<>();

   @Inject
   public ClientRequestHandler(
      Logger logger, IExternalBlockWatcherManager blockWatcherManager, IPlayerDataManager playerDataManager, IPortalClient portalClient
   ) {
      this.logger = logger;
      this.blockWatcherManager = blockWatcherManager;
      this.playerDataManager = playerDataManager;
      this.portalClient = portalClient;
   }

   public void handlePendingRequests() {
      while (!this.awaitingHandling.isEmpty()) {
         Pair<Request, Consumer<Response>> next = this.awaitingHandling.remove();
         this.handleRequestInternal(next.first(), next.second());
      }
   }

   @Override
   public void handleRequest(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
      this.awaitingHandling.add(new Pair<>(request, onFinish));
   }

   private void handleRequestInternal(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
      this.logger.finer("Processing request of type: %s", request.getClass().getName());

      try {
         if (request instanceof RelayRequest) {
            this.handleRelayedRequest((RelayRequest)request, onFinish);
         } else if (request instanceof GetBlockDataChangesRequest) {
            this.handleGetBlockDataChangesRequest((GetBlockDataChangesRequest)request, onFinish);
         } else if (request instanceof TestForwardedRequest) {
            this.handleTestForwardedRequest((TestForwardedRequest)request, onFinish);
         } else if (request instanceof CheckDestinationValidityRequest) {
            this.handleCheckDestinationValidityRequest((CheckDestinationValidityRequest)request, onFinish);
         } else if (request instanceof TeleportRequest) {
            this.handleTeleportRequest((TeleportRequest)request, onFinish);
         } else if (request instanceof GetSelectionRequest) {
            this.handleGetSelectionRequest((GetSelectionRequest)request, onFinish);
         } else {
            if (!(request instanceof PreviousServerPutRequest)) {
               throw new IllegalStateException("Received request of unknown type");
            }

            this.handlePreviousServerPutRequest((PreviousServerPutRequest)request, onFinish);
         }
      } catch (RequestException var5) {
         Response response = new Response();
         response.setError(var5);
         onFinish.accept(response);
      } catch (Exception var6) {
         Response responsex = new Response();
         responsex.setError(new RequestException(var6, "Internal error occurred on a client server while processing request"));
         onFinish.accept(responsex);
      }
   }

   private void handleRelayedRequest(RelayRequest request, Consumer<Response> onFinish) throws IOException, ClassNotFoundException {
      Request innerRequest = request.getInnerRequest();
      this.handleRequest(innerRequest, response -> {
         ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

         try {
            new ObjectOutputStream(byteOutputStream).writeObject(response);
         } catch (IOException var4) {
         }

         Response wrappedResponse = new Response();
         wrappedResponse.setResult(byteOutputStream.toByteArray());
         onFinish.accept(wrappedResponse);
      });
   }

   private void handleGetBlockDataChangesRequest(@NotNull GetBlockDataChangesRequest request, @NotNull Consumer<Response> onFinish) {
      this.blockWatcherManager.onRequestReceived(request, onFinish);
   }

   private void handleTestForwardedRequest(@NotNull TestForwardedRequest request, @NotNull Consumer<Response> onFinish) throws RequestException {
      this.logger.info("Received test forwarded request. Content: %s", request.getTestField());
      Response response = new Response();
      response.setResult(request.getTestField().add(new IntVector(0, 10, 0)));
      onFinish.accept(response);
   }

   private void handleCheckDestinationValidityRequest(@NotNull CheckDestinationValidityRequest request, @NotNull Consumer<Response> onFinish) throws RequestException {
      String gameVersion = VersionUtil.getCurrentVersion();
      if (!gameVersion.equals(request.getOriginGameVersion())) {
         throw new RequestException(
            String.format(
               "Origin and destination servers are not on the same game version (%s on the destination vs %s on the origin)",
               gameVersion,
               request.getOriginGameVersion()
            )
         );
      } else if (Bukkit.getWorld(request.getDestinationWorldName()) == null && Bukkit.getWorld(request.getDestinationWorldId()) == null) {
         throw new RequestException("Destination world no longer exists");
      } else {
         onFinish.accept(new Response());
      }
   }

   private void handleTeleportRequest(@NotNull TeleportRequest request, @NotNull Consumer<Response> onFinish) {
      this.playerDataManager.setTeleportOnJoin(request);
      onFinish.accept(new Response());
   }

   private void handleGetSelectionRequest(@NotNull GetSelectionRequest request, @NotNull Consumer<Response> onFinish) throws RequestException {
      Response response = new Response();
      IPortalSelection destSelection = this.playerDataManager.getDestinationSelectionWhenLoggedOut(request.getPlayerId());
      response.setResult(destSelection != null && destSelection.isValid() ? new GetSelectionRequest.ExternalSelectionInfo(destSelection) : null);
      this.logger.fine("Returning selection %s", destSelection);
      onFinish.accept(response);
   }

   private void handlePreviousServerPutRequest(@NotNull PreviousServerPutRequest request, @NotNull Consumer<Response> onFinish) throws RequestException {
      String previousServer = request.getPreviousServer();
      GetSelectionRequest getSelectionRequest = new GetSelectionRequest();
      getSelectionRequest.setPlayerId(request.getPlayerId());
      this.logger.fine("Previous server: %s", previousServer);
      this.portalClient.sendRequestToServer(getSelectionRequest, previousServer, response -> {
         try {
            GetSelectionRequest.ExternalSelectionInfo selectionInfo = (GetSelectionRequest.ExternalSelectionInfo)response.getResult();
            if (selectionInfo != null) {
               selectionInfo.getPosition().setServerName(previousServer);
            }

            this.logger.fine("Selection info: %s", selectionInfo);
            this.playerDataManager.setExternalSelectionOnLogin(request.getPlayerId(), selectionInfo);
            onFinish.accept(new Response());
         } catch (RequestException var6) {
            this.logger.warning("An error occurred while trying to sync destination selection for player with ID %s", request.getPlayerId());
            var6.printStackTrace();
         }
      });
   }
}
