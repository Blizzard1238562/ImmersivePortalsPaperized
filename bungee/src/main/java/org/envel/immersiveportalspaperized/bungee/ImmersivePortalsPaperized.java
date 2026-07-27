package org.envel.immersiveportalspaperized.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.envel.immersiveportalspaperized.proxy.net.IPortalServer;
import org.envel.immersiveportalspaperized.proxy.net.PortalServer;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.DisconnectNotice;

public class ImmersivePortalsPaperized extends Plugin {
   @Inject
   private Logger logger;
   @Inject
   private Config config;
   private IPortalServer portalServer;
   private boolean didEnableFail = false;

   public void onEnable() {
      Injector injector = Guice.createInjector(new MainModule(this));
      DisconnectNotice forceClassLoad = new DisconnectNotice();
      this.logger.finest(forceClassLoad.toString());

      try {
         this.config.load();
      } catch (Exception e) {
         this.logger.severe("Failed to load the config file");
         this.logger.severe("Please check that your YAML syntax is correct");
         this.logger.severe("%s", e.getMessage());
         this.didEnableFail = true;
         return;
      }

      this.portalServer = injector.getInstance(PortalServer.class);
      injector.getInstance(ServerSwitch.class);
      this.portalServer.startUp();
   }

   public void onDisable() {
      if (!this.didEnableFail) {
         this.portalServer.shutDown();
      }
   }
}
