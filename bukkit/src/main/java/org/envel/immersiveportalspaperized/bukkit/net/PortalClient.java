package org.envel.immersiveportalspaperized.bukkit.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.crypto.AEADBadTagException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.config.ProxyConfig;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import org.envel.immersiveportalspaperized.bukkit.util.VersionUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.DisconnectNotice;
import org.envel.immersiveportalspaperized.shared.net.Handshake;
import org.envel.immersiveportalspaperized.shared.net.HandshakeResponse;
import org.envel.immersiveportalspaperized.shared.net.IRequestHandler;
import org.envel.immersiveportalspaperized.shared.net.RequestException;
import org.envel.immersiveportalspaperized.shared.net.Response;
import org.envel.immersiveportalspaperized.shared.net.encryption.CipherManager;
import org.envel.immersiveportalspaperized.shared.net.encryption.EncryptedObjectStreamFactory;
import org.envel.immersiveportalspaperized.shared.net.encryption.IEncryptedObjectStream;
import org.envel.immersiveportalspaperized.shared.net.requests.RelayRequest;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

@Singleton
public class PortalClient implements IPortalClient {
   private final JavaPlugin pl;
   private final ProxyConfig proxyConfig;
   private final Logger logger;
   private final EncryptedObjectStreamFactory encryptedObjectStreamFactory;
   private final IRequestHandler requestHandler;
   private final IClientReconnectHandler reconnectHandler;
   private final CipherManager cipherManager;
   private Socket socket;
   private volatile boolean isRunning = false;
   private volatile boolean hasHandshakeFinished = false;
   private volatile boolean shouldReconnectIfFailed;
   private IEncryptedObjectStream objectStream;
   private final AtomicInteger currentRequestId = new AtomicInteger();
   private final ConcurrentMap<Integer, Consumer<Response>> waitingRequests = new ConcurrentHashMap<>();

   @Inject
   public PortalClient(
      JavaPlugin pl,
      ProxyConfig proxyConfig,
      Logger logger,
      CipherManager cipherManager,
      EncryptedObjectStreamFactory encryptedObjectStreamFactory,
      IRequestHandler requestHandler,
      IClientReconnectHandler reconnectHandler
   ) {
      this.pl = pl;
      this.proxyConfig = proxyConfig;
      this.logger = logger;
      this.encryptedObjectStreamFactory = encryptedObjectStreamFactory;
      this.requestHandler = requestHandler;
      this.reconnectHandler = reconnectHandler;
      this.cipherManager = cipherManager;
   }

   @Override
   public void connect(boolean printErrors) {
      if (this.isRunning) {
         throw new IllegalStateException("Attempted to start connection when was was already established");
      } else {
         this.isRunning = true;
         this.shouldReconnectIfFailed = true;

         try {
            this.cipherManager.init(this.proxyConfig.getEncryptionKey());
         } catch (NoSuchAlgorithmException var3) {
            this.logger.severe("Unable to find algorithm to encrypt proxy connection");
            var3.printStackTrace();
         }

         new Thread(() -> {
            try {
               this.run();
               return;
            } catch (IOException var8) {
               if (this.isRunning) {
                  if (printErrors) {
                     this.logger.warning("An IO error occurred while connected to the proxy");
                     this.logger.warning("%s: %s", var8.getClass().getName(), var8.getMessage());
                  }

                  return;
               }
            } catch (AEADBadTagException var9) {
               this.shouldReconnectIfFailed = false;
               if (printErrors) {
                  this.logger.warning("Failed to initialise encryption with the proxy");
                  this.logger.warning("Please make sure that your encryption key is valid!");
                  var9.printStackTrace();
               }

               return;
            } catch (Exception var10) {
               if (printErrors) {
                  this.logger.warning("An error occurred while connected to the proxy");
                  var10.printStackTrace();
               }

               return;
            } finally {
               this.disconnect();
            }
         }).start();
      }
   }

   private void run() throws IOException, GeneralSecurityException, ClassNotFoundException {
      this.socket = new Socket();
      this.socket.connect(this.proxyConfig.getAddress());
      this.socket.setKeepAlive(this.proxyConfig.isKeepAlive());
      this.logger.fine("Hello from client thread");
      this.objectStream = this.encryptedObjectStreamFactory.create(this.socket.getInputStream(), this.socket.getOutputStream());
      if (!this.runHandshake()) {
         this.shouldReconnectIfFailed = false;
      } else {
         this.logger.info("Successfully connected to the proxy");

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
      this.requestHandler.handleRequest(request, response -> SchedulerUtil.runAsync(() -> {
         response.setId(request.getId());

         try {
            this.send(response);
         } catch (GeneralSecurityException | IOException var4) {
            this.logger.warning("IO Error occurred while sending a response to a request");
            var4.printStackTrace();
            this.disconnect();
         }
      }));
   }

   private void processResponse(Response response) {
      Consumer<Response> waiter = this.waitingRequests.remove(response.getId());
      if (waiter == null) {
         throw new IllegalStateException("Received response for request that didn't exist");
      } else {
         SchedulerUtil.runTask(() -> waiter.accept(response));
      }
   }

   private boolean runHandshake() throws IOException, GeneralSecurityException, ClassNotFoundException {
      this.logger.fine("Running handshake . . .");
      Handshake handshake = new Handshake();
      handshake.setPluginVersion(this.pl.getDescription().getVersion());
      handshake.setServerPort(Bukkit.getPort());
      handshake.setGameVersion(VersionUtil.getCurrentVersion());
      handshake.setOverrideServerName(this.proxyConfig.getOverrideServerName());
      this.objectStream.writeObject(handshake);
      HandshakeResponse response = (HandshakeResponse)this.objectStream.readObject();
      return switch (response.getStatus()) {
         case SUCCESS -> {
            this.logger.fine("Handshake was successful");
            this.hasHandshakeFinished = true;
            yield true;
         }
         case PLUGIN_VERSION_MISMATCH -> {
            this.logger.severe("Bukkit plugin & proxy plugin versions are different. Please update both to the latest version");
            yield false;
         }
         case SERVER_NOT_REGISTERED -> {
            this.logger
               .severe(
                  "Proxy reported that this server wasn't registered on their end. This happens if the server you're connecting from isn't registered with bungeecord"
               );
            yield false;
         }
      };
   }

   @Override
   public void shutDown() {
      if (this.isRunning) {
         this.reconnectHandler.stop();
         this.isRunning = false;
         this.hasHandshakeFinished = false;
         this.shouldReconnectIfFailed = false;

         try {
            if (this.objectStream != null) {
               this.send(new DisconnectNotice());
            }
         } catch (GeneralSecurityException | IOException var2) {
            this.logger.warning("Error occurred while sending disconnection notice to proxy");
            var2.printStackTrace();
         }

         this.disconnect(true);
      }
   }

   @Override
   public boolean canReceiveRequests() {
      return this.hasHandshakeFinished;
   }

   @Override
   public boolean isConnectionOpen() {
      return this.isRunning;
   }

   @Override
   public boolean getShouldReconnect() {
      return this.shouldReconnectIfFailed;
   }

   private void disconnect() {
      this.disconnect(false);
   }

   private void disconnect(boolean force) {
      if (this.isRunning || force) {
         if (this.hasHandshakeFinished || force) {
            this.logger.info("Disconnecting from the proxy");
         }

         this.isRunning = false;
         this.hasHandshakeFinished = false;

         try {
            if (this.socket != null) {
               this.socket.close();
            }
         } catch (IOException var5) {
            this.logger.warning("Error occurred while closing proxy connection socket");
            var5.printStackTrace();
         }

         Response disconnectResponse = new Response();
         disconnectResponse.setError(new RequestException("Disconnected from proxy while sending the request"));

         for (Consumer<Response> responseConsumer : this.waitingRequests.values()) {
            responseConsumer.accept(disconnectResponse);
         }

         this.waitingRequests.clear();
         this.reconnectHandler.onClientDisconnect();
      }
   }

   @Override
   public void sendRequestToProxy(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
      int requestId = this.currentRequestId.getAndIncrement();
      request.setId(requestId);
      this.waitingRequests.put(requestId, onFinish);
      if (!this.hasHandshakeFinished) {
         Response notConnected = new Response();
         notConnected.setError(new RequestException("Not connected to the proxy"));
         onFinish.accept(notConnected);
      } else {
         SchedulerUtil.runAsync(() -> {
            try {
               this.send(request);
            } catch (GeneralSecurityException | IOException var3x) {
               this.logger.warning("Disconnected from proxy while sending request");
               this.disconnect();
            }
         });
      }
   }

   @Override
   public void sendRequestToServer(@NotNull Request request, @NotNull String destinationServer, @NotNull Consumer<Response> onFinish) {
      RelayRequest relayRequest = new RelayRequest();
      relayRequest.setInnerRequest(request);
      relayRequest.setDestination(destinationServer);
      this.sendRequestToProxy(relayRequest, response -> {
         try {
            byte[] responseData = (byte[])response.getResult();
            Object deserializedResponse = new ObjectInputStream(new ByteArrayInputStream(responseData)).readObject();
            onFinish.accept((Response)deserializedResponse);
         } catch (RequestException var5) {
            Response eResponse = new Response();
            eResponse.setError(var5);
            onFinish.accept(eResponse);
         } catch (ClassNotFoundException | IOException var6) {
            this.disconnect();
         }
      });
   }

   public synchronized void send(Object obj) throws GeneralSecurityException, IOException {
      this.objectStream.writeObject(obj);
   }
}
