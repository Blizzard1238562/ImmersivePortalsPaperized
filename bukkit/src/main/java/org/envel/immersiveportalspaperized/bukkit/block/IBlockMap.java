package org.envel.immersiveportalspaperized.bukkit.block;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import java.util.List;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IBlockMap {
   void update(int ticksSinceActivated);

   @Nullable
   List<IViewableBlockInfo> getViewableStates();

   @Nullable
   WrapperPlayServerBlockEntityData getOriginTileEntityPacket(@NotNull IntVector position);

   @Nullable
   WrapperPlayServerBlockEntityData getDestinationTileEntityPacket(@NotNull IntVector position);

   void reset();

   public interface Factory {
      IBlockMap create(IPortal portal);
   }
}
