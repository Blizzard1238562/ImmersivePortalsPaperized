package org.envel.immersiveportalspaperized.bukkit.block.lighting;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import jakarta.inject.Singleton;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.Nullable;

@Singleton
public class DummyLightDataManager implements ILightDataManager {
   @Nullable
   @Override
   public WrappedBlockData getLightData(IPortal portal) {
      return null;
   }
}
