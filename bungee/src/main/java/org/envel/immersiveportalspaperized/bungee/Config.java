package org.envel.immersiveportalspaperized.bungee;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.proxy.IProxyConfig;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * Loads BungeeCord-specific configuration from {@code config.yml}.
 */
@Singleton
public class Config implements IProxyConfig {
   private final Plugin pl;
   private final Logger logger;
   @Getter
   private InetSocketAddress bindAddress;
   @Getter
   private UUID key;

   @Inject
   public Config(Plugin pl, Logger logger) {
      this.pl = pl;
      this.logger = logger;
   }

   private Path getConfigFilePath() {
      File dataFolder = this.pl.getDataFolder();
      dataFolder.mkdir();
      return dataFolder.toPath().resolve("config.yml");
   }

   private Configuration loadFile() throws IOException {
      Path configFilePath = this.getConfigFilePath();
      File configFile = configFilePath.toFile();
      if (!configFile.exists()) {
         this.logger.info("Saving default config . . .");
         Files.copy(this.pl.getResourceAsStream("bungeeconfig.yml"), configFilePath);
      }

      return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
   }

   private void saveFile(Configuration file) throws IOException {
      ConfigurationProvider.getProvider(YamlConfiguration.class).save(file, this.getConfigFilePath().toFile());
   }

   public void load() throws IOException {
      Configuration config = this.loadFile();
      boolean wasModified = false;
      if (!config.contains("logLevel")) {
         config.set("logLevel", "INFO");
         config.set("enableDebugLogging", null);
         wasModified = true;
      }

      Level configuredLevel = Level.parse(config.getString("logLevel"));
      this.logger.setLevel(configuredLevel);
      String addressStr = config.getString("bindAddress");
      if (addressStr == null) {
         throw new RuntimeException("Missing bind address");
      } else {
         int port = config.getInt("serverPort");
         if (port == 0) {
            throw new RuntimeException("Invalid bind port " + port);
         } else {
            try {
               this.key = UUID.fromString(Objects.requireNonNull(config.getString("key"), "No encryption key found in the config"));
            } catch (IllegalArgumentException var7) {
               this.logger.info("Generating new random encryption key");
               this.key = UUID.randomUUID();
               config.set("key", this.key.toString());
               wasModified = true;
            }

            this.bindAddress = new InetSocketAddress(addressStr, port);
            if (wasModified) {
               this.saveFile(config);
            }
         }
      }
   }
}
