package org.envel.immersiveportalspaperized.bukkit.config;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * RenderConfig.
 */
@Singleton
@Getter
public class RenderConfig {
   private final Logger logger;
   private double minXZ;
   private double maxXZ;
   private double minY;
   private double maxY;
   private int yMultip;
   private int zMultip;
   private int totalArrayLength;
   private IntVector halfFullSize;
   private WrappedBlockState backgroundBlockData;
   private final Map<String, WrappedBlockState> worldBackgroundBlockData = new HashMap<>();
   private int[] intOffsets;
   private Vector collisionBox;
   private int blockUpdateInterval;
   private int worldSwitchWaitTime;
   private boolean portalBlocksHidden;
   private int blockStateRefreshInterval;
   private int entityMetadataUpdateInterval;
   private int lightSimulationInterval;
   private int forceLightLevel;

   @Inject
   public RenderConfig(Logger logger) {
      this.logger = logger;
   }

   @Nullable
   private WrappedBlockState parseBlockData(String str) {
      try {
         return SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(Material.valueOf(str.toUpperCase(Locale.ROOT))));
      } catch (IllegalArgumentException var3) {
         this.logger.warning("Unknown material for portal edge block " + str);
         this.logger.warning("Using default of black concrete");
         return null;
      }
   }

   public void load(FileConfiguration file) {
      this.maxXZ = file.getInt("portalEffectSizeXZ");
      this.minXZ = this.maxXZ * -1.0;
      this.maxY = file.getInt("portalEffectSizeY");
      this.minY = this.maxY * -1.0;
      if (!(this.maxXZ <= 0.0) && !(this.maxY <= 0.0)) {
         this.zMultip = (int)(this.maxXZ - this.minXZ + 1.0);
         this.yMultip = this.zMultip * this.zMultip;
         this.totalArrayLength = this.yMultip * (int)(this.maxY - this.minY + 1.0);
         this.lightSimulationInterval = file.getInt("lightBlockInterval");
         this.forceLightLevel = file.getInt("forceLightLevel");
         this.halfFullSize = new IntVector((this.maxXZ - this.minXZ) / 2.0, (this.maxY - this.minY) / 2.0, (this.maxXZ - this.minXZ) / 2.0);
         ConfigurationSection cBoxSection = Objects.requireNonNull(file.getConfigurationSection("portalCollisionBox"), "Collision box missing");
         this.collisionBox = new Vector(cBoxSection.getDouble("x"), cBoxSection.getDouble("y"), cBoxSection.getDouble("z"));
         this.blockUpdateInterval = file.getInt("portalBlockUpdateInterval");
         if (this.blockUpdateInterval <= 0) {
            throw new IllegalArgumentException("Block update interval must be at least 1");
         } else {
            this.entityMetadataUpdateInterval = file.getInt("entityMetadataUpdateInterval");
            this.worldSwitchWaitTime = file.getInt("waitTimeAfterSwitchingWorlds");
            this.portalBlocksHidden = file.getBoolean("hidePortalBlocks");
            this.blockStateRefreshInterval = file.getInt("blockStateRefreshInterval");
            String bgBlockString = file.getString("backgroundBlock", "");
            if (bgBlockString.isEmpty()) {
               this.backgroundBlockData = null;
            } else {
               this.backgroundBlockData = this.parseBlockData(bgBlockString);
            }

            this.worldBackgroundBlockData.clear();
            ConfigurationSection worldBgsSection = file.getConfigurationSection("worldBackgroundBlocks");

            for (String worldName : Objects.requireNonNull(worldBgsSection).getKeys(false)) {
               String bgValue = worldBgsSection.getString(worldName);
               if (bgValue != null) {
                  WrappedBlockState parsedData = this.parseBlockData(bgValue);
                  if (parsedData != null) {
                     this.worldBackgroundBlockData.put(worldName, parsedData);
                  }
               }
            }

            this.intOffsets = new int[]{1, -1, this.zMultip, -this.zMultip, this.yMultip, -this.yMultip};
         }
      } else {
         throw new IllegalArgumentException("The portal effect size must be at least one");
      }
   }

   public boolean isOutsideBounds(int x, int y, int z) {
      return x <= this.minXZ || x >= this.maxXZ || y <= this.minY || y >= this.maxY || z <= this.minXZ || z >= this.maxXZ;
   }

   public WrappedBlockState findBackgroundData(PortalPosition destPosition) {
      if (this.backgroundBlockData != null) {
         return this.backgroundBlockData;
      } else {
         WrappedBlockState worldSpecificBg = this.worldBackgroundBlockData.get(destPosition.getWorldName());
         if (worldSpecificBg != null) {
            return worldSpecificBg;
         } else if (destPosition.isExternal()) {
            return SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(Material.BLACK_CONCRETE));
         } else {
            World world = destPosition.getWorld();
            if (world == null) {
               return SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(Material.BLACK_CONCRETE));
            }

            Material material;
            if (world.getEnvironment() == Environment.NORMAL) {
               long time = world.getTime();
               if (time > 0L && time < 12300L) {
                  material = Material.WHITE_CONCRETE;
               } else {
                  material = Material.BLACK_CONCRETE;
               }
            } else if (world.getEnvironment() == Environment.NETHER) {
               material = Material.RED_CONCRETE;
            } else {
               material = Material.BLACK_CONCRETE;
            }

            return SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(material));
         }
      }
   }
}


