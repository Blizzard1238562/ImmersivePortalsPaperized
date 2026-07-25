package org.envel.immersiveportalspaperized.bukkit.config;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class ProxyConfig {
   private final Logger logger;
   @Getter
   private boolean isEnabled;
   @Getter
   private InetSocketAddress address;
   @Getter
   private UUID encryptionKey;
   @Getter
   private int reconnectionDelay;
   @Getter
   private boolean warnOnMissingSelection;
   @Getter
   public String overrideServerName;
   @Getter
   public boolean keepAlive;

   @Inject
   public ProxyConfig(Logger logger) {
      this.logger = logger;
   }

   public void load(FileConfiguration config) {
      ConfigurationSection section = Objects.requireNonNull(config.getConfigurationSection("proxy"), "Proxy section missing");
      this.isEnabled = section.getBoolean("enableProxy");
      if (this.isEnabled) {
         String rawAddress = Objects.requireNonNull(section.getString("proxyAddress"), "Proxy address missing");
         int port = section.getInt("proxyPort");
         this.address = new InetSocketAddress(rawAddress, port);
         this.reconnectionDelay = section.getInt("reconnectionDelay");
         if (this.reconnectionDelay <= 0) {
            throw new IllegalArgumentException("reconnectionDelay must be greater than 0 (got " + this.reconnectionDelay + ")");
         } else {
            this.overrideServerName = section.getString("serverName");
            if (this.overrideServerName != null && this.overrideServerName.isEmpty()) {
               this.overrideServerName = null;
            }

            String legacyOverrideOption = section.getString("overrideServerName");
            if (legacyOverrideOption != null && legacyOverrideOption.isEmpty()) {
               legacyOverrideOption = null;
            }

            if (legacyOverrideOption != null && this.overrideServerName == null) {
               this.overrideServerName = legacyOverrideOption;
            } else if (this.overrideServerName == null) {
               this.logger
                  .warning(
                     "No server name set in the BP proxy config. It is highly recommended to set this value to the server name in the bungeecord config to avoid issues with the proxy determining which server is connecting."
                  );
               this.logger.info("You can set this by adding the server name in the field called 'serverName'");
            }

            try {
               this.encryptionKey = UUID.fromString(Objects.requireNonNull(section.getString("key"), "Encryption key missing"));
            } catch (IllegalArgumentException var7) {
               this.logger.warning("Failed to load encryption key from config file! Please make sure you set this to the key in the bungeecord config.");
               this.isEnabled = false;
            }

            this.warnOnMissingSelection = section.getBoolean("warnOnMissingSelection");
            this.keepAlive = section.getBoolean("keepAlive", true);
         }
      }
   }
}
