package org.envel.immersiveportalspaperized.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import com.google.inject.AbstractModule;
import org.envel.immersiveportalspaperized.proxy.IProxy;
import org.envel.immersiveportalspaperized.proxy.IProxyConfig;
import org.envel.immersiveportalspaperized.proxy.net.ProxyModule;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.logging.OverrideLogger;

/**
 * Guice bindings for the BungeeCord module.
 */
public class MainModule extends AbstractModule {
   private final ImmersivePortalsPaperized pl;

   public MainModule(ImmersivePortalsPaperized pl) {
      this.pl = pl;
   }

   @Override
   public void configure() {
      this.bind(Plugin.class).toInstance(this.pl);
      this.bind(IProxyConfig.class).to(Config.class);
      this.bind(IProxy.class).to(BungeeProxy.class);
      this.install(new ProxyModule());
      this.bind(Logger.class).toInstance(new OverrideLogger(this.pl.getLogger()));
   }
}
