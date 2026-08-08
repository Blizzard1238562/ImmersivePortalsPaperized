package org.envel.immersiveportalspaperized.bukkit.block.bukkit;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.block.IMultiBlockChangeManager;
import org.envel.immersiveportalspaperized.bukkit.block.IViewableBlockInfo;
import org.envel.immersiveportalspaperized.bukkit.nms.PacketUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * ModernMultiBlockChangeManager.
 */
public class ModernMultiBlockChangeManager implements IMultiBlockChangeManager {
   private final Player player;
   private final int minChunkY;
   private final int maxChunkY;
   private final HashMap<Vector3i, Map<Vector, WrappedBlockState>> changes = new HashMap<>();

   @Inject
   public ModernMultiBlockChangeManager(@Assisted Player player, @Assisted("minChunkY") int minChunkY, @Assisted("maxChunkY") int maxChunkY) {
      this.player = player;
      this.minChunkY = minChunkY;
      this.maxChunkY = maxChunkY;
   }

   @Override
   public void addChange(Vector position, WrappedBlockState newData) {
      Vector3i sectionPosition = new Vector3i(position.getBlockX() >> 4, position.getBlockY() >> 4, position.getBlockZ() >> 4);
      Map<Vector, WrappedBlockState> existingList = this.changes.computeIfAbsent(sectionPosition, k -> new HashMap<>());
      existingList.put(position, newData);
   }

   @Override
   public void addChangeOrigin(Vector position, IViewableBlockInfo newData) {
      this.addChange(position, ((BukkitBlockInfo)newData).getOriginData());
   }

   @Override
   public void addChangeDestination(Vector position, IViewableBlockInfo newData) {
      this.addChange(position, ((BukkitBlockInfo)newData).getRenderedDestData());
   }

   @Override
   public void sendChanges() {
      // WrapperPlayServerMultiBlockChange.EncodedBlock(WrappedBlockState, x, y, z) takes GLOBAL
      // block coordinates (confirmed against packetevents-api 2.13.0 javadoc), not chunk-relative
      // ones - the wrapper itself derives the relative position from the chunk section position.
      // WrapperPlayServerMultiBlockChange's constructor takes EncodedBlock[], not a List.
      for (Entry<Vector3i, Map<Vector, WrappedBlockState>> entry : this.changes.entrySet()) {
         Vector3i sectionPosition = entry.getKey();
         int chunkY = sectionPosition.getY();
         if (chunkY <= this.maxChunkY && chunkY >= this.minChunkY) {
            List<WrapperPlayServerMultiBlockChange.EncodedBlock> encodedBlocks = new ArrayList<>();
            for (Entry<Vector, WrappedBlockState> blockEntry : entry.getValue().entrySet()) {
               Vector pos = blockEntry.getKey();
               encodedBlocks.add(
                  new WrapperPlayServerMultiBlockChange.EncodedBlock(
                     blockEntry.getValue(), pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()
                  )
               );
            }

            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(
               sectionPosition, true, encodedBlocks.toArray(new WrapperPlayServerMultiBlockChange.EncodedBlock[0])
            );
            PacketUtil.sendPacket(this.player, packet);
         }
      }
   }
}


