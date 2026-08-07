package org.envel.immersiveportalspaperized.bukkit.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * ConfigManager.
 */
@Singleton
public class ConfigManager {
   private final MessageConfig messages;
   private final PortalSpawnConfig spawning;
   private final RenderConfig rendering;
   private final ProxyConfig proxy;
   private final MiscConfig misc;
   private final Logger logger;

   @Inject
   public ConfigManager(Logger logger, MessageConfig messages, PortalSpawnConfig spawning, RenderConfig rendering, ProxyConfig proxy, MiscConfig misc) {
      this.logger = logger;
      this.messages = messages;
      this.spawning = spawning;
      this.rendering = rendering;
      this.proxy = proxy;
      this.misc = misc;
   }

   public void loadValues(@NotNull FileConfiguration file, @Nullable JavaPlugin pl) {
      if (pl != null) {
         file = this.updateFromResources(file, pl);
      }

      this.misc.load(file);
      this.messages.load(file);
      this.spawning.load(file);
      this.rendering.load(file);
      this.proxy.load(file);
   }

   private String readResourceToString(@NotNull JavaPlugin pl, @NotNull String name) {
      InputStream resource = pl.getResource(name);
      if (resource == null) {
         return null;
      } else {
         BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
         StringBuilder buffer = new StringBuilder();

         String str;
         try {
            while ((str = reader.readLine()) != null) {
               buffer.append(str);
               buffer.append("\n");
            }
         } catch (IOException e) {
            this.logger.warning("Failed to read resource %s: %s", name, e.getMessage());
            return null;
         }

         return buffer.toString();
      }
   }

   private int evaluateKeyCount(Set<String> allKeys, FileConfiguration file) {
      int deepKeyCount = allKeys.size();
      ConfigurationSection worldConnections = file.getConfigurationSection("worldConnections");
      if (worldConnections == null) {
         return deepKeyCount;
      } else {
         int worldConnectionKeyCount = worldConnections.getKeys(true).size();
         return deepKeyCount - worldConnectionKeyCount;
      }
   }

   private FileConfiguration updateFromResources(FileConfiguration file, JavaPlugin pl) {
      FileConfiguration defaultConfig = new YamlConfiguration();

      try {
         defaultConfig.loadFromString(
            Objects.requireNonNull(this.readResourceToString(pl, "config.yml"), "Failed to read default config resource - this should never happen!")
         );
         Set<String> savedFileKeys = file.getKeys(true);
         int savedKeyCount = this.evaluateKeyCount(savedFileKeys, file);
         int defaultKeyCount = this.evaluateKeyCount(defaultConfig.getKeys(true), defaultConfig);
         this.logger.fine("Saved keys: %d, Default keys: %d", savedKeyCount, defaultKeyCount);
         if (savedKeyCount >= defaultKeyCount) {
            return file;
         }

         this.logger.info("Updating config file . . .");

         for (String key : savedFileKeys) {
            Object value = file.get(key);
            if (!(value instanceof ConfigurationSection)) {
               defaultConfig.set(key, value);
            }
         }

         defaultConfig.save(pl.getDataFolder().toPath().resolve("config.yml").toFile());
      } catch (Exception e) {
         this.logger.warning("Failed to update config file: %s", e.getMessage());
      }

      return defaultConfig;
   }
}


