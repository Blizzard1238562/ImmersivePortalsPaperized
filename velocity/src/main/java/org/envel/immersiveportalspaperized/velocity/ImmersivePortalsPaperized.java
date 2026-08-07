package org.envel.immersiveportalspaperized.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import jakarta.inject.Inject;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.envel.immersiveportalspaperized.proxy.net.IClientHandler;
import org.envel.immersiveportalspaperized.proxy.net.IPortalServer;
import org.envel.immersiveportalspaperized.shared.net.RequestException;
import org.envel.immersiveportalspaperized.shared.net.requests.PreviousServerPutRequest;
import org.slf4j.Logger;

/**
 * Velocity plugin entry point. Bootstraps Guice and starts the portal server.
 */
public class ImmersivePortalsPaperized {
   private final Injector injector;
   private final Logger logger;
   private IPortalServer portalServer;

   @Inject
   public ImmersivePortalsPaperized(MainModule mainModule, Logger logger) {
      this.injector = Guice.createInjector(mainModule);
      this.logger = logger;
   }

   @Subscribe
   public void onProxyInitialization(ProxyInitializeEvent event) {
      try {
         this.injector.getInstance(Config.class).load();
      } catch (Exception var4) {
         this.logger.error("Failed to load config file", var4);
         return;
      }

      try {
         this.portalServer = this.injector.getInstance(IPortalServer.class);
         this.portalServer.startUp();
      } catch (RuntimeException var3) {
         this.logger.error("Failed to start up portal server", var3);
      }
   }

   @Subscribe
   public void onProxyShutdown(ProxyShutdownEvent event) {
      if (this.portalServer != null) {
         try {
            this.portalServer.shutDown();
         } catch (RuntimeException var3) {
            this.logger.error("Failed to shut down portal server", var3);
         }
      }
   }

   @Subscribe
   public void onServerSwitch(ServerConnectedEvent event) {
      if (this.portalServer != null) {
         if (event.getPreviousServer().isPresent()) {
            IClientHandler to = this.portalServer.getServer(event.getServer().getServerInfo().getName());
            String previousServerName = ((RegisteredServer)event.getPreviousServer().get()).getServerInfo().getName();
            IClientHandler from = this.portalServer.getServer(previousServerName);
            if (from == null) {
               this.logger.debug("From server was unregistered for server switch event, skipping");
            } else if (to == null) {
               this.logger.debug("To server was unregistered for server switch event, skipping");
            } else {
               this.logger.debug("Sending previous server put request");
               PreviousServerPutRequest request = new PreviousServerPutRequest();
               request.setPlayerId(event.getPlayer().getUniqueId());
               request.setPreviousServer(previousServerName);
               to.sendRequest(request, response -> {
                  try {
                     response.checkForErrors();
                  } catch (RequestException e) {
                     this.logger.warn("Failed to set previous server for player {}", event.getPlayer().getUniqueId(), e);
                  }
               });
            }
         }
      }
   }
}
