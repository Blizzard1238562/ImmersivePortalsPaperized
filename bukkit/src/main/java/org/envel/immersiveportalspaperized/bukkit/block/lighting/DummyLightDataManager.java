package org.envel.immersiveportalspaperized.bukkit.block.lighting;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import jakarta.inject.Singleton;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.Nullable;

@Singleton
public class DummyLightDataManager implements ILightDataManager {
   @Nullable
   @Override
   public WrappedBlockState getLightData(IPortal portal) {
      return null;
   }
}
