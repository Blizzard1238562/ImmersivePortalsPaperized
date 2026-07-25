package org.envel.immersiveportalspaperized.bukkit.config;

import java.util.Objects;
import java.util.logging.Level;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class MiscConfig {
   private final Logger logger;
   @Getter
   private double portalActivationDistance;
   @Getter
   private boolean entitySupportEnabled;
   @Getter
   private int entityCheckInterval;
   @Getter
   private int teleportCooldown;
   @Getter
   private boolean updateCheckEnabled;
   @Getter
   private boolean testingCommandsEnabled;
   @Getter
   private int portalSaveInterval;
   @Getter
   private int maxPortalsPerPlayer;
   @Getter
   private boolean preventDuplicatePortals;
   @Getter
   private double minTpsForRendering;

   @Inject
   public MiscConfig(Logger logger) {
      this.logger = logger;
   }

   public void load(FileConfiguration config) {
      this.portalActivationDistance = config.getDouble("portalActivationDistance");
      if (this.portalActivationDistance < 0.0) {
         throw new IllegalArgumentException("portalActivationDistance must be at least 0.0 (got " + this.portalActivationDistance + ")");
      } else {
         this.entitySupportEnabled = config.getBoolean("enableEntitySupport");
         boolean disableEntityCheckInterval = config.getBoolean("checkForEntitiesEveryTick");
         this.entityCheckInterval = disableEntityCheckInterval ? 1 : config.getInt("entityCheckInterval");
         if (this.entityCheckInterval <= 0) {
            throw new IllegalArgumentException("entityCheckInterval must be greater than 0 (got " + this.entityCheckInterval + ")");
         } else {
            this.updateCheckEnabled = config.getBoolean("enableUpdateCheck");

            Level logLevel;
            try {
               logLevel = Level.parse(Objects.requireNonNull(config.getString("logLevel"), "Logging level missing"));
            } catch (NullPointerException | IllegalArgumentException var5) {
               this.logger.warning("Invalid logging level found in the config");
               this.logger.warning("Defaulting to INFO");
               logLevel = Level.INFO;
            }

            this.logger.setLevel(logLevel);
            this.teleportCooldown = config.getInt("teleportCooldown");
            if (this.teleportCooldown < 0) {
               throw new IllegalArgumentException("teleportCooldown must be at least 0 (got " + this.teleportCooldown + ")");
            } else {
               this.testingCommandsEnabled = config.getBoolean("enableTestingCommands");
               this.portalSaveInterval = config.getInt("portalSaveInterval");
               if (this.portalSaveInterval <= 0) {
                  throw new IllegalArgumentException("portalSaveInterval must be greater than 0 (got " + this.portalSaveInterval + ")");
               } else {
                  this.maxPortalsPerPlayer = config.getInt("maxPortalsPerPlayer", 3);
                  this.preventDuplicatePortals = config.getBoolean("preventDuplicatePortals", true);
                  this.minTpsForRendering = config.getDouble("minTpsForRendering", 19.0);
               }
            }
         }
      }
   }
}
