package org.envel.immersiveportalspaperized.bukkit.block.lighting;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.Nullable;

public interface ILightDataManager {
   @Nullable
   WrappedBlockData getLightData(IPortal portal);
}
