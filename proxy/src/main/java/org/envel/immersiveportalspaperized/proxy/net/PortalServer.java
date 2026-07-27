package org.envel.immersiveportalspaperized.proxy.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.proxy.IProxyConfig;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.encryption.CipherManager;

@Singleton
public class PortalServer implements IPortalServer {
   private final Logger logger;
   private final IProxyConfig config;
   private final IClientHandler.Factory serverHandlerFactory;
   private final Set<IClientHandler> connectedServers = new HashSet<>();
   private final Map<String, IClientHandler> registeredServers = new ConcurrentHashMap<>();
   private ServerSocket serverSocket;
   private volatile boolean isRunning = false;

   @Inject
   public PortalServer(Logger logger, CipherManager cipherManager, IProxyConfig config, IClientHandler.Factory serverHandlerFactory) throws Exception {
      this.logger = logger;
      this.config = config;
      this.serverHandlerFactory = serverHandlerFactory;
      cipherManager.init(config.getKey());
   }

   @Override
   public void startUp() {
      if (this.isRunning) {
         throw new IllegalStateException("Attempted to start server when it was already running");
      } else {
         this.isRunning = true;
         this.logger.info("Starting up portal server");
         Thread serverThread = new Thread(() -> {
            this.logger.fine("Hello from server thread");

            try {
               this.runServer();
               return;
            } catch (IOException e) {
               this.logger.fine("Caught IO Error on server thread");
               if (this.isRunning) {
                  this.logger.warning("An IO error occurred while running the portal server: %s", e.getMessage());
                  return;
               }
            } catch (Exception e) {
               this.logger.warning("An error occurred while running the portal server: %s", e.getMessage());
               return;
            } finally {
               this.shutDown();
            }
         });
         serverThread.setName("ImmersivePortalsPaperized-PortalServer");
         serverThread.start();
      }
   }

   private void runServer() throws IOException {
      this.serverSocket = new ServerSocket();
      this.serverSocket.bind(this.config.getBindAddress());

      while (this.isRunning) {
         this.logger.fine("Awaiting new connections");
         Socket next = this.serverSocket.accept();
         this.logger.fine("Received connection from %s", next.getRemoteSocketAddress());
         IClientHandler handler = this.serverHandlerFactory.create(next);
         this.connectedServers.add(handler);
      }
   }

   @Override
   public void shutDown() {
      if (this.isRunning) {
         this.logger.info("Shutting down portal server");
         this.isRunning = false;

         try {
            this.serverSocket.close();

            for (IClientHandler serverHandler : new ArrayList<>(this.connectedServers)) {
               serverHandler.shutDown();
            }
         } catch (IOException e) {
            this.logger.warning("An IO error occurred while shutting down the portal server: %s", e.getMessage());
         }
      }
   }

   @Override
   public void registerServer(@NotNull IClientHandler serverHandler, @NotNull String serverName) {
      if (!this.connectedServers.contains(serverHandler)) {
         throw new IllegalArgumentException("Attempted to register server that wasn't connected");
      } else {
         this.registeredServers.put(serverName, serverHandler);
      }
   }

   @Override
   public void onServerDisconnect(@NotNull IClientHandler handler) {
      this.connectedServers.remove(handler);
      String serverName = handler.getServerName();
      if (serverName != null) {
         this.logger.finer("Server %s disconnected from the portal server", serverName);
         this.registeredServers.remove(serverName);
      } else {
         this.logger.finer("Unregistered server disconnected from the portal server");
      }
   }

   @Nullable
   @Override
   public IClientHandler getServer(@NotNull String name) {
      return this.registeredServers.get(name);
   }
}
