package org.envel.immersiveportalspaperized.bukkit.block.lighting;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.Nullable;

/**
 * ILightDataManager.
 */
public interface ILightDataManager {
   @Nullable
   WrappedBlockState getLightData(IPortal portal);
}


