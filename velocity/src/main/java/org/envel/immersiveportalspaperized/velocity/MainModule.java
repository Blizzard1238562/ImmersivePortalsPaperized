package org.envel.immersiveportalspaperized.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.util.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.envel.immersiveportalspaperized.proxy.IProxy;
import org.envel.immersiveportalspaperized.proxy.IProxyConfig;
import org.envel.immersiveportalspaperized.proxy.net.ProxyModule;
import org.slf4j.Logger;

/**
 * Guice bindings for the Velocity module.
 */
public class MainModule extends AbstractModule {
   private final ProxyServer proxyServer;
   private final Path dataDirectory;
   private final Logger logger;
   private final String pluginVersion;

   @Inject
   public MainModule(ProxyServer proxyServer, @DataDirectory Path dataDirectory, PluginContainer pluginContainer, Logger logger) {
      this.proxyServer = proxyServer;
      this.dataDirectory = dataDirectory;
      this.logger = logger;
      Optional<String> version = pluginContainer.getDescription().getVersion();
      if (!version.isPresent()) {
         throw new IllegalStateException("Plugin had no version");
      } else {
         this.pluginVersion = version.get();
      }
   }

   @Override
   protected void configure() {
      this.install(new ProxyModule());
      this.bind(IProxyConfig.class).to(Config.class);
      this.bind(ProxyServer.class).toInstance(this.proxyServer);
      this.bind(IProxy.class).to(VelocityProxy.class);
      this.bind(String.class).annotatedWith(Names.named("pluginVersion")).toInstance(this.pluginVersion);
      this.bind(Path.class).annotatedWith(Names.named("dataDirectory")).toInstance(this.dataDirectory);
      this.bind(org.envel.immersiveportalspaperized.shared.logging.Logger.class).toInstance(new VelocityLogger(this.logger));
   }
}
