package org.envel.immersiveportalspaperized.bukkit.block.lighting;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.data.type.Light;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.Nullable;

@Singleton
public class LightDataManger implements ILightDataManager {
   private final RenderConfig renderConfig;

   @Inject
   public LightDataManger(RenderConfig renderConfig) {
      this.renderConfig = renderConfig;
   }

   private int getLightLevel(IPortal portal) {
      if (this.renderConfig.getForceLightLevel() >= 0) {
         return this.renderConfig.getForceLightLevel();
      } else {
         World destWorld = portal.getDestPos().getWorld();
         if (destWorld == null) {
            return -1;
         } else {
            Environment destEnv = destWorld.getEnvironment();
            if (destEnv == Environment.NETHER) {
               return 8;
            } else if (destEnv == Environment.NORMAL) {
               if (portal.getDestPos().getVector().getY() > 64.0) {
                  long time = destWorld.getTime();
                  return time > 0L && time < 12300L ? 15 : -1;
               } else {
                  return -1;
               }
            } else {
               return -1;
            }
         }
      }
   }

   @Nullable
   @Override
   public WrappedBlockState getLightData(IPortal portal) {
      int lightLevel = this.getLightLevel(portal);
      if (lightLevel == -1) {
         return null;
      } else {
         Light lightBlockData = (Light)Bukkit.createBlockData(Material.LIGHT);
         lightBlockData.setLevel(lightLevel);
         return SpigotConversionUtil.fromBukkitBlockData(lightBlockData);
      }
   }
}
