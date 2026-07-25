package org.envel.immersiveportalspaperized.bukkit.portal.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.portal.Portal;
import com.google.inject.Inject;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class YamlPortalStorage extends IPortalStorage {
   private final JavaPlugin pl;
   private final IPortalManager portalManager;
   private final Logger logger;
   private final LegacyPortalLoader legacyPortalLoader;

   @Inject
   public YamlPortalStorage(JavaPlugin pl, Logger logger, IPortalManager portalManager, MiscConfig miscConfig, LegacyPortalLoader legacyPortalLoader) {
      super(logger, pl, miscConfig);
      this.pl = pl;
      this.logger = logger;
      this.portalManager = portalManager;
      this.legacyPortalLoader = legacyPortalLoader;
      ConfigurationSerialization.registerClass(Portal.class);
      ConfigurationSerialization.registerClass(PortalPosition.class, "org.envel.immersiveportalspaperized.bukkit.portal.PortalPosition");
   }

   private Path getDataFolder() {
      File pluginFolder = this.pl.getDataFolder();
      pluginFolder.mkdir();
      File dataFolder = pluginFolder.toPath().resolve("data").toFile();
      dataFolder.mkdir();
      return dataFolder.toPath();
   }

   private File getPortalsFile() {
      return this.getDataFolder().resolve("portals.yml").toFile();
   }

   private FileConfiguration loadPortalsFile() {
      this.logger.fine("Loading from plugins/ImmersivePortalsPaperized/data/portals.yml");
      return YamlConfiguration.loadConfiguration(this.getPortalsFile());
   }

   private void savePortalsFile(FileConfiguration configFile) throws IOException {
      this.logger.fine("Saving to plugins/ImmersivePortalsPaperized/data/portals.yml");
      configFile.save(this.getPortalsFile());
   }

   @Override
   public void loadPortals() {
      FileConfiguration file = this.loadPortalsFile();
      ConfigurationSection portalsSection = file.getConfigurationSection("portals");
      if (portalsSection == null) {
         this.logger.fine("The portals file was empty, stopping!");
      } else {
         Set<String> portalNumbers = portalsSection.getKeys(false);
         this.logger.finer("Loading %d portals from parsed YAML . . .", portalNumbers.size());

         for (String portalNumber : portalNumbers) {
            ConfigurationSection portalSection = portalsSection.getConfigurationSection(portalNumber);

            IPortal newPortal;
            try {
               if (portalSection != null && portalSection.contains("portalPosition")) {
                  this.logger.finer("Loading legacy portal.");
                  newPortal = this.legacyPortalLoader.loadLegacyPortal(portalSection);
               } else {
                  this.logger.finer("Loading modern portal.");
                  newPortal = (IPortal)portalsSection.get(portalNumber);
               }
            } catch (RuntimeException var9) {
               this.logger.warning("Failed to load portal: %s", var9.getMessage());
               continue;
            }

            if (newPortal.getOriginPos().getWorld() == null) {
               this.pl
                  .getLogger()
                  .warning(
                     String.format("Portal at position %s, was not loaded because the world it was in no longer exists!", newPortal.getOriginPos().getVector())
                  );
            } else {
               this.portalManager.registerPortal(newPortal);
            }
         }

         this.logger.fine("Loaded portals");
      }
   }

   @Override
   public void savePortals() throws IOException {
      FileConfiguration file = new YamlConfiguration();
      ConfigurationSection portalsSection = file.createSection("portals");
      this.logger.fine("Saving all portals . . .");
      int i = 0;

      for (IPortal portal : this.portalManager.getAllPortals()) {
         try {
            portalsSection.set(String.valueOf(i), portal);
         } catch (RuntimeException var7) {
            this.logger.warning("Failed to save portal: %s", var7.getMessage());
         }

         i++;
      }

      this.logger.fine("Saved %d portals", i);
      this.savePortalsFile(file);
   }
}
