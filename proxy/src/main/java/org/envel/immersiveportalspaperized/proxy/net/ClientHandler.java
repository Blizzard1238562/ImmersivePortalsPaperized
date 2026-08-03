package org.envel.immersiveportalspaperized.proxy.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.crypto.AEADBadTagException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.proxy.IProxy;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.DisconnectNotice;
import org.envel.immersiveportalspaperized.shared.net.Handshake;
import org.envel.immersiveportalspaperized.shared.net.HandshakeResponse;
import org.envel.immersiveportalspaperized.shared.net.IRequestHandler;
import org.envel.immersiveportalspaperized.shared.net.RequestException;
import org.envel.immersiveportalspaperized.shared.net.Response;
import org.envel.immersiveportalspaperized.shared.net.encryption.EncryptedObjectStreamFactory;
import org.envel.immersiveportalspaperized.shared.net.encryption.IEncryptedObjectStream;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

/**
 * Manages the encrypted object stream and request routing for one connected Bukkit server.
 */
public class ClientHandler implements IClientHandler {
   private final IPortalServer portalServer;
   private final Logger logger;
   private final EncryptedObjectStreamFactory encryptedObjectStreamFactory;
   private final IRequestHandler requestHandler;
   private final Socket socket;
   private IEncryptedObjectStream objectStream;
   @Getter
   private String serverName = null;
   @Getter
   private String gameVersion;
   private final IProxy proxy;
   private volatile boolean isRunning = true;
   private final AtomicInteger currentRequestId = new AtomicInteger();
   private final ConcurrentMap<Integer, Consumer<Response>> waitingRequests = new ConcurrentHashMap<>();

   @Inject
   public ClientHandler(
      @Assisted Socket socket,
      IPortalServer portalServer,
      Logger logger,
      EncryptedObjectStreamFactory encryptedObjectStreamFactory,
      IRequestHandler requestHandler,
      IProxy proxy
   ) {
      this.socket = socket;
      this.portalServer = portalServer;
      this.logger = logger;
      this.encryptedObjectStreamFactory = encryptedObjectStreamFactory;
      this.requestHandler = requestHandler;
      this.proxy = proxy;
      new Thread(() -> {
         try {
            this.run();
            return;
         } catch (IOException e) {
            if (this.isRunning) {
               if (e.getCause() instanceof AEADBadTagException) {
                  this.printEncryptionFailure();
               } else {
                  logger.warning("An IO error occurred while connected to %s", socket.getRemoteSocketAddress());
                  logger.warning("%s: %s", e.getClass().getName(), e.getMessage());
               }

               return;
            }
         } catch (AEADBadTagException e) {
            this.printEncryptionFailure();
            return;
         } catch (Exception e) {
            logger.warning("An error occurred while connected to %s", socket.getRemoteSocketAddress());
            logger.warning("%s: %s", e.getClass().getName(), e.getMessage());
            return;
         } finally {
            this.disconnect();
         }
      }).start();
   }

   private void printEncryptionFailure() {
      this.logger.warning("Failed to initialise encryption with %s", this.socket.getRemoteSocketAddress());
      this.logger.warning("Please make sure that your encryption key is valid!");
   }

   private boolean performHandshake() throws IOException, ClassNotFoundException, GeneralSecurityException {
      this.logger.fine("Reading handshake . . .");
      Handshake handshake = (Handshake)this.objectStream.readObject();
      this.logger.fine("Handshake plugin version: %s. Handshake game version: %s", handshake.getPluginVersion(), handshake.getGameVersion());
      HandshakeResponse.Result result = HandshakeResponse.Result.SUCCESS;
      if (!this.proxy.getPluginVersion().equals(handshake.getPluginVersion())) {
         this.logger.warning("A server tried to register with a different plugin version (%s)", handshake.getPluginVersion());
         result = HandshakeResponse.Result.PLUGIN_VERSION_MISMATCH;
      }

      InetSocketAddress statedServerAddress = new InetSocketAddress(this.socket.getInetAddress(), handshake.getServerPort());
      String serverName = handshake.getOverrideServerName();
      if (serverName != null) {
         this.logger.finer("Using manually stated server name %s", serverName);
         if (!this.proxy.serverExists(serverName)) {
            this.logger.warning("Server name %s was stated by a client, but no listed server existed with that name!", serverName);
            serverName = null;
         }
      }

      if (serverName == null) {
         this.logger.warning("Finding server info from socket address and port, this behaviour is deprecated!");
         serverName = this.proxy.findServer(statedServerAddress);
      }

      if (serverName == null) {
         this.logger.warning("A server tried to register that didn't exist on the proxy!");
         result = HandshakeResponse.Result.SERVER_NOT_REGISTERED;
      }

      HandshakeResponse response = new HandshakeResponse();
      response.setStatus(result);
      this.send(response);
      if (result == HandshakeResponse.Result.SUCCESS) {
         this.logger.fine("Successfully registered with server %s", serverName);
         this.logger.fine("Plugin version: %s. Game version: %s.", handshake.getPluginVersion(), handshake.getGameVersion());
         this.portalServer.registerServer(this, serverName);
         this.serverName = serverName;
         this.gameVersion = handshake.getGameVersion();
         return true;
      } else {
         return false;
      }
   }

   private void run() throws IOException, ClassNotFoundException, GeneralSecurityException {
      this.objectStream = this.encryptedObjectStreamFactory.create(this.socket.getInputStream(), this.socket.getOutputStream());
      if (this.performHandshake()) {
         while (true) {
            Object next = this.objectStream.readObject();
            if (next instanceof DisconnectNotice) {
               this.logger.fine("Received disconnection notice, shutting down!");
               return;
            }

            if (next instanceof Response) {
               this.processResponse((Response)next);
            } else if (next instanceof Request) {
               this.processRequest((Request)next);
            }
         }
      }
   }

   private void processRequest(Request request) {
      int requestId = request.getId();
      this.requestHandler.handleRequest(request, response -> {
         response.setId(requestId);

         try {
            this.send(response);
         } catch (GeneralSecurityException | IOException e) {
            this.logger.warning("IO Error occurred while sending a response to a request: %s", e.getMessage());
            this.disconnect();
         }
      });
   }

   private void processResponse(Response response) {
      Consumer<Response> waiter = this.waitingRequests.remove(response.getId());
      if (waiter == null) {
         throw new IllegalStateException("Received response for request that didn't exist");
      } else {
         waiter.accept(response);
      }
   }

   @Override
   public void shutDown() {
      if (this.isRunning) {
         try {
            this.send(new DisconnectNotice());
         } catch (GeneralSecurityException | IOException var2) {
            this.logger.warning("Error occurred while sending disconnection notice to %s", this.socket.getRemoteSocketAddress());
         }

         this.disconnect();
      }
   }

   private void disconnect() {
      if (this.isRunning) {
         this.isRunning = false;
         this.portalServer.onServerDisconnect(this);

         try {
            this.socket.close();
         } catch (IOException e) {
            this.logger.warning("Error occurred while disconnecting from %s: %s", this.socket.getRemoteSocketAddress(), e.getMessage());
         }

         Response disconnectResponse = new Response();
         disconnectResponse.setError(new RequestException("Client server connection disconnected while sending the request"));

         for (Consumer<Response> responseConsumer : this.waitingRequests.values()) {
            responseConsumer.accept(disconnectResponse);
         }
      }
   }

   private synchronized void send(Object obj) throws IOException, GeneralSecurityException {
      this.objectStream.writeObject(obj);
   }

   private void verifyCanSendRequests() {
      if (this.serverName == null) {
         throw new IllegalStateException("Attempted to send request before handshake was finished");
      }
   }

   @Override
   public void sendRequest(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
      this.verifyCanSendRequests();
      int requestId = this.currentRequestId.getAndIncrement();
      request.setId(requestId);
      this.waitingRequests.put(requestId, onFinish);

      try {
         this.send(request);
      } catch (GeneralSecurityException | IOException var5) {
         this.logger.warning("Client server connection disconnected while sending the request");
         this.disconnect();
      }
   }
}
