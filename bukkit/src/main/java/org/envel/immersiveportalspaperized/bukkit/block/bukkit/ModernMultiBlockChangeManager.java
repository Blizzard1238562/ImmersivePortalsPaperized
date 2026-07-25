package org.envel.immersiveportalspaperized.bukkit.block.bukkit;

import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.block.IMultiBlockChangeManager;
import org.envel.immersiveportalspaperized.bukkit.block.IViewableBlockInfo;
import org.envel.immersiveportalspaperized.bukkit.nms.PacketUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class ModernMultiBlockChangeManager implements IMultiBlockChangeManager {
   private final Player player;
   private final int minChunkY;
   private final int maxChunkY;
   private final HashMap<BlockPosition, Map<Vector, WrappedBlockData>> changes = new HashMap<>();

   @Inject
   public ModernMultiBlockChangeManager(@Assisted Player player, @Assisted("minChunkY") int minChunkY, @Assisted("maxChunkY") int maxChunkY) {
      this.player = player;
      this.minChunkY = minChunkY;
      this.maxChunkY = maxChunkY;
   }

   @Override
   public void addChange(Vector position, WrappedBlockData newData) {
      BlockPosition sectionPosition = new BlockPosition(position.getBlockX() >> 4, position.getBlockY() >> 4, position.getBlockZ() >> 4);
      Map<Vector, WrappedBlockData> existingList = this.changes.computeIfAbsent(sectionPosition, k -> new HashMap<>());
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

   private short getShortLocation(Vector vec) {
      int x = vec.getBlockX() & 15;
      int y = vec.getBlockY() & 15;
      int z = vec.getBlockZ() & 15;
      return (short)(x << 8 | z << 4 | y);
   }

   @Override
   public void sendChanges() {
      for (Entry<BlockPosition, Map<Vector, WrappedBlockData>> entry : this.changes.entrySet()) {
         PacketContainer packet = new PacketContainer(Server.MULTI_BLOCK_CHANGE);
         int chunkY = entry.getKey().getY();
         if (chunkY <= this.maxChunkY && chunkY >= this.minChunkY) {
            packet.getSectionPositions().write(0, entry.getKey());
            int blockCount = entry.getValue().size();
            WrappedBlockData[] data = new WrappedBlockData[blockCount];
            short[] positions = new short[blockCount];
            int i = 0;

            for (Entry<Vector, WrappedBlockData> blockEntry : entry.getValue().entrySet()) {
               positions[i] = this.getShortLocation(blockEntry.getKey());
               data[i] = blockEntry.getValue();
               i++;
            }

            packet.getBlockDataArrays().writeSafely(0, data);
            packet.getShortArrays().writeSafely(0, positions);
            PacketUtil.sendPacket(this.player, packet);
         }
      }
   }
}
