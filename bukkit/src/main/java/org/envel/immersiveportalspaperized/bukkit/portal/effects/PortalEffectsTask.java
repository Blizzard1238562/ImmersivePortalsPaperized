package org.envel.immersiveportalspaperized.bukkit.portal.effects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * PortalEffectsTask.
 */
@Singleton
public class PortalEffectsTask implements Runnable {
   private static final double SOUND_RADIUS_SQUARED = 2500.0;
   private final JavaPlugin plugin;
   private final IPortalManager portalManager;
   private final Map<String, PortalEffectPreset> presets = new HashMap<>();
   private SchedulerUtil.PortalTask task;
   private long tickCount = 0L;

   public Collection<String> getPresetNames() {
      return this.presets.keySet();
   }

   public PortalEffectPreset getPreset(String name) {
      return this.presets.get(name.toLowerCase());
   }

   @Inject
   public PortalEffectsTask(JavaPlugin plugin, IPortalManager portalManager) {
      this.plugin = plugin;
      this.portalManager = portalManager;
      this.loadPresets();
   }

   public void loadPresets() {
      this.presets.clear();
      FileConfiguration config = this.plugin.getConfig();
      ConfigurationSection section = config.getConfigurationSection("portalEffects");
      if (section != null) {
         for (String key : section.getKeys(false)) {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection != null) {
               this.presets.put(key.toLowerCase(), new PortalEffectPreset(key, subSection));
            }
         }
      }

      if (!this.presets.containsKey("default")) {
         config.addDefault("portalEffects.default.particle.type", "PORTAL");
         config.addDefault("portalEffects.default.particle.count", 3);
         config.addDefault("portalEffects.default.particle.speed", 0.05);
         config.addDefault("portalEffects.default.sound.type", "BLOCK_PORTAL_AMBIENT");
         config.addDefault("portalEffects.default.sound.volume", 0.15);
         config.addDefault("portalEffects.default.sound.pitch", 1.0);
         config.addDefault("portalEffects.default.sound.interval", 80);
         this.plugin.saveConfig();
         ConfigurationSection defSection = config.getConfigurationSection("portalEffects.default");
         if (defSection != null) {
            this.presets.put("default", new PortalEffectPreset("default", defSection));
         }
      }
   }

   public void start() {
      if (this.task != null) {
         this.task.cancel();
      }

      this.task = SchedulerUtil.runTaskTimer(this, 0L, 10L);
   }

   public void stop() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }
   }

   @Override
   public void run() {
      this.tickCount += 10L;

      for (IPortal portal : this.portalManager.getAllPortals()) {
         String presetName = portal.getEffectPreset();
         if (presetName == null) {
            presetName = "default";
         }

         PortalEffectPreset preset = this.presets.get(presetName.toLowerCase());
         if (preset == null) {
            preset = this.presets.get("default");
         }

         if (preset != null) {
            Location loc = portal.getOriginPos().getLocation();
            World world = loc.getWorld();
            if (world != null) {
               world.spawnParticle(
                  preset.getParticle(),
                  loc.getX(),
                  loc.getY(),
                  loc.getZ(),
                  preset.getParticleCount(),
                  preset.getOffsetX(),
                  preset.getOffsetY(),
                  preset.getOffsetZ(),
                  preset.getParticleSpeed()
               );
               if (portal.isSoundEnabled() && preset.getSound() != null && this.tickCount % preset.getSoundIntervalTicks() == 0L) {
                  for (Player player : world.getPlayers()) {
                     if (player.getLocation().distanceSquared(loc) < SOUND_RADIUS_SQUARED) {
                        player.playSound(loc, preset.getSound(), preset.getSoundVolume(), preset.getSoundPitch());
                     }
                  }
               }
            }
         }
      }
   }
}


