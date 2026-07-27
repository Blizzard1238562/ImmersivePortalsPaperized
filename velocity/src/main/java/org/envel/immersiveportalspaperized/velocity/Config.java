package org.envel.immersiveportalspaperized.velocity;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import lombok.Getter;
import com.google.inject.Inject;
import org.envel.immersiveportalspaperized.proxy.IProxyConfig;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class Config implements IProxyConfig {
   private final Path dataDirectory;
   private final Logger logger;
   @Getter
   private InetSocketAddress bindAddress;
   @Getter
   private UUID key;

   @Inject
   public Config(@Named("dataDirectory") Path dataDirectory, Logger logger) {
      this.dataDirectory = dataDirectory;
      this.logger = logger;
   }

   private Path getConfigFilePath() {
      File dataFolder = this.dataDirectory.toFile();
      dataFolder.mkdir();
      return dataFolder.toPath().resolve("config.toml");
   }

   private Toml loadFile() throws IOException {
      Path configFilePath = this.getConfigFilePath();
      File configFile = configFilePath.toFile();
      if (!configFile.exists()) {
         this.logger.info("Saving default config . . .");
         InputStream defaultConfig = this.getClass().getResourceAsStream("/velocityconfig.toml");
         if (defaultConfig == null) {
            throw new IllegalStateException("Could not find default config file!");
         }

         Files.copy(defaultConfig, configFilePath);
      }

      return new Toml().read(configFile);
   }

   private void saveFile(Map<String, Object> config) throws IOException {
      TomlWriter tomlWriter = new TomlWriter();
      tomlWriter.write(config, this.getConfigFilePath().toFile());
   }

   public void load() throws IOException {
      Toml configFile = this.loadFile();
      boolean wasModified = false;
      Map<String, Object> configMap = configFile.toMap();
      if (!configMap.containsKey("logLevel")) {
         configMap.put("logLevel", "INFO");
         configMap.remove("enableDebugLogging");
         wasModified = true;
      }

      Level configuredLevel = Level.parse((String)configMap.get("logLevel"));
      this.logger.setLevel(configuredLevel);
      String addressStr = configFile.getString("bindAddress");
      if (addressStr == null) {
         throw new RuntimeException("Missing bind address");
      } else {
         int port = Math.toIntExact(configFile.getLong("serverPort"));
         if (port == 0) {
            throw new RuntimeException("Invalid bind port " + port);
         } else {
            try {
               this.key = UUID.fromString(Objects.requireNonNull((String)configMap.get("key"), "No encryption key found in the config"));
            } catch (IllegalArgumentException var8) {
               this.logger.info("Generating new random encryption key");
               this.key = UUID.randomUUID();
               configMap.put("key", this.key.toString());
               wasModified = true;
            }

            this.bindAddress = new InetSocketAddress(addressStr, port);
            if (wasModified) {
               this.saveFile(configMap);
            }
         }
      }
   }
}
