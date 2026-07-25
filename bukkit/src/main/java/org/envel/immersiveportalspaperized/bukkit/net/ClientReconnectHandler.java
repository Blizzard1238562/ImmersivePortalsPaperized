package org.envel.immersiveportalspaperized.bukkit.net;

import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.config.ProxyConfig;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class ClientReconnectHandler implements IClientReconnectHandler, Runnable {
   private final JavaPlugin pl;
   private final ProxyConfig proxyConfig;
   private final IPortalClient portalClient;
   private final Logger logger;
   private volatile SchedulerUtil.PortalTask reconnectWorker;
   private boolean isFirstReconnectionAttempt;

   @Inject
   public ClientReconnectHandler(JavaPlugin pl, ProxyConfig proxyConfig, IPortalClient portalClient, Logger logger) {
      this.pl = pl;
      this.proxyConfig = proxyConfig;
      this.portalClient = portalClient;
      this.logger = logger;
   }

   @Override
   public void prematureReconnect() {
      this.portalClient.connect(true);
   }

   @Override
   public void stop() {
      if (this.reconnectWorker != null) {
         this.reconnectWorker.cancel();
         this.reconnectWorker = null;
      }
   }

   @Override
   public void onClientDisconnect() {
      if (this.reconnectWorker == null) {
         if (this.portalClient.getShouldReconnect()) {
            int reconnectionDelay = this.proxyConfig.getReconnectionDelay();
            if (reconnectionDelay != -1) {
               this.logger.info("Scheduling reconnection attempt in %d ticks", reconnectionDelay);
               this.isFirstReconnectionAttempt = true;
               this.reconnectWorker = SchedulerUtil.runTaskTimer(this, reconnectionDelay, reconnectionDelay);
            }
         }
      }
   }

   @Override
   public void run() {
      if (this.portalClient.canReceiveRequests()) {
         this.logger.fine("Proxy is now connected! Stopping . . .");
         this.reconnectWorker.cancel();
         this.reconnectWorker = null;
      } else if (this.portalClient.isConnectionOpen()) {
         this.logger.fine("Previous reconnection attempt still ongoing");
      } else {
         if (this.isFirstReconnectionAttempt) {
            this.logger.info("Processing reconnection attempt to proxy");
         } else {
            this.logger.fine("Processing reconnection attempt to proxy");
         }

         this.portalClient.connect(this.isFirstReconnectionAttempt);
         if (this.isFirstReconnectionAttempt) {
            this.isFirstReconnectionAttempt = false;
            this.logger.info("NOTE: Subsequent reconnection attempts will not print to console");
         }
      }
   }
}
