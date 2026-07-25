package org.envel.immersiveportalspaperized.bukkit.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class PortalSpawnConfig {
   private static final int MAIN_Y_PADDING = 5;
   private static final double MAIN_RESCALE_FACTOR = 8.0;
   private final Logger logger;
   private Map<World, WorldLink> worldLinks;
   private Set<World> disabledWorlds;
   @Getter
   private Vector maxPortalSize;
   @Getter
   private int minimumPortalSpawnDistance;
   @Getter
   private boolean dimensionBlendEnabled;
   @Getter
   private double blendFallOff;
   @Getter
   private double allowedSpawnTimePerTick;

   @Inject
   public PortalSpawnConfig(Logger logger) {
      this.logger = logger;
   }

   public void load(FileConfiguration file) {
      this.worldLinks = new HashMap<>();
      this.disabledWorlds = new HashSet<>();
      ConfigurationSection dimBlendSection = Objects.requireNonNull(file.getConfigurationSection("dimensionBlend"));
      this.dimensionBlendEnabled = dimBlendSection.getBoolean("enable");
      this.blendFallOff = dimBlendSection.getDouble("fallOffRate");
      ConfigurationSection worldLinksSection = Objects.requireNonNull(file.getConfigurationSection("worldConnections"), "World connections section missing");

      for (String s : worldLinksSection.getKeys(false)) {
         WorldLink newLink = new WorldLink(Objects.requireNonNull(worldLinksSection.getConfigurationSection(s)));
         if (!newLink.isValid()) {
            this.logger.warning("An invalid worldConnection was found in the config, please check that your world names are correct.");
            if (newLink.getOriginWorld() == null) {
               this.logger.warning("No world with name \"%s\" exists (for the origin)", newLink.getOriginWorldName());
            }

            if (newLink.getDestinationWorld() == null) {
               this.logger.warning("No world with name \"%s\" exists (for the destination)", newLink.getDestWorldName());
            }
         } else {
            this.worldLinks.put(newLink.getOriginWorld(), newLink);
         }
      }

      boolean useDefaultWorldLinks = file.getBoolean("enableDefaultWorldConnections");
      if (useDefaultWorldLinks) {
         this.generateDefaultLinks();
      }

      for (String worldString : file.getStringList("disabledWorlds")) {
         World world = Bukkit.getWorld(worldString);
         this.disabledWorlds.add(world);
      }

      ConfigurationSection maxSizeSection = Objects.requireNonNull(file.getConfigurationSection("maxPortalSize"), "Maximum portal size section missing");
      this.maxPortalSize = new Vector(maxSizeSection.getInt("x"), maxSizeSection.getInt("y"), 0.0);
      this.minimumPortalSpawnDistance = file.getInt("minimumPortalSpawnDistance");
      if (this.minimumPortalSpawnDistance < 0) {
         throw new IllegalArgumentException("minimumPortalSpawnDistance must be at least 0 (got " + this.minimumPortalSpawnDistance + ")");
      } else {
         this.allowedSpawnTimePerTick = file.getDouble("allowedSpawnTimePerTick");
         if (this.allowedSpawnTimePerTick <= 0.0) {
            throw new IllegalArgumentException("allowedSpawnTimePerTick must be greater than 0.0 (got " + this.allowedSpawnTimePerTick + ")");
         }
      }
   }

   private void generateDefaultLinks() {
      World mainWorld = (World)Bukkit.getWorlds().get(0);
      World netherWorld = Bukkit.getWorld(mainWorld.getName() + "_nether");
      if (netherWorld == null) {
         this.logger.warning("Cannot add default world links - no nether world exists");
      } else if (mainWorld.getEnvironment() != Environment.NORMAL) {
         this.logger.warning("Cannot add default world links - first world is not overworld");
      } else {
         if (netherWorld.getEnvironment() != Environment.NETHER) {
            this.logger.warning("Cannot add default world links - _nether world is not actually nether");
         }

         this.worldLinks.put(mainWorld, new WorldLink(mainWorld, netherWorld, 1.0 / MAIN_RESCALE_FACTOR, MAIN_Y_PADDING));
         this.worldLinks.put(netherWorld, new WorldLink(netherWorld, mainWorld, MAIN_RESCALE_FACTOR, MAIN_Y_PADDING));
      }
   }

   public boolean isWorldDisabled(World world) {
      return this.disabledWorlds.contains(world);
   }

   @Nullable
   public WorldLink getWorldLink(@NotNull World originWorld) {
      return this.worldLinks.get(originWorld);
   }
}
