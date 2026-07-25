package org.envel.immersiveportalspaperized.bukkit.block;

import com.comphenix.protocol.events.PacketContainer;
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
   PacketContainer getOriginTileEntityPacket(@NotNull IntVector position);

   @Nullable
   PacketContainer getDestinationTileEntityPacket(@NotNull IntVector position);

   void reset();

   public interface Factory {
      IBlockMap create(IPortal portal);
   }
}
