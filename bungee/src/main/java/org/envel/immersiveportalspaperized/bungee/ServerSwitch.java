package org.envel.immersiveportalspaperized.bungee;

import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import com.google.inject.Inject;
import org.envel.immersiveportalspaperized.proxy.net.IClientHandler;
import org.envel.immersiveportalspaperized.proxy.net.IPortalServer;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.RequestException;
import org.envel.immersiveportalspaperized.shared.net.requests.PreviousServerPutRequest;

public class ServerSwitch implements Listener {
   private final IPortalServer portalServer;
   private final Logger logger;

   @Inject
   public ServerSwitch(IPortalServer portalServer, Logger logger, Plugin pl) {
      this.portalServer = portalServer;
      this.logger = logger;
      pl.getProxy().getPluginManager().registerListener(pl, this);
   }

   @EventHandler
   public void onServerSwitch(ServerSwitchEvent event) {
      this.logger.finer("Found server switch event for user %s", event.getPlayer().getUniqueId());
      if (event.getFrom() == null) {
         this.logger.finer("From server is null, skipping");
      } else {
         IClientHandler from = this.portalServer.getServer(event.getFrom().getName());
         IClientHandler to = this.portalServer.getServer(event.getPlayer().getServer().getInfo().getName());
         if (from == null) {
            this.logger.finer("From server was unregistered for server switch event, skipping");
         } else if (to == null) {
            this.logger.finer("To server was unregistered for server switch event, skipping");
         } else {
            this.logger.finer("Sending previous server put request");
            PreviousServerPutRequest request = new PreviousServerPutRequest();
            request.setPlayerId(event.getPlayer().getUniqueId());
            request.setPreviousServer(event.getFrom().getName());
            to.sendRequest(request, response -> {
               try {
                  this.logger.finer("Sent and received response");
                  response.checkForErrors();
               } catch (RequestException e) {
                  this.logger.warning("Failed to set previous server for player %s: %s", event.getPlayer().getUniqueId(), e.getMessage());
               }
            });
         }
      }
   }
}
