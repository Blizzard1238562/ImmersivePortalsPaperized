package org.envel.immersiveportalspaperized.bukkit.block.bukkit;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.block.FloodFillBlockMap;
import org.envel.immersiveportalspaperized.bukkit.block.IViewableBlockInfo;
import org.envel.immersiveportalspaperized.bukkit.block.fetch.BlockDataFetcherFactory;
import org.envel.immersiveportalspaperized.bukkit.block.fetch.IBlockDataFetcher;
import org.envel.immersiveportalspaperized.bukkit.block.lighting.ILightDataManager;
import org.envel.immersiveportalspaperized.bukkit.block.rotation.IBlockRotator;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.envel.immersiveportalspaperized.bukkit.nms.BlockDataUtil;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.util.MaterialUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class BukkitBlockMap extends FloodFillBlockMap {
   private final IBlockRotator blockRotator;
   private final BlockDataFetcherFactory dataFetcherFactory;
   private final Matrix rotateDestToOrigin;
   private IBlockDataFetcher dataFetcher;
   private final World originWorld;
   private final ILightDataManager lightDataManager;
   private WrappedBlockData wrappedLightData;

   @Inject
   public BukkitBlockMap(
      @Assisted IPortal portal,
      Logger logger,
      RenderConfig renderConfig,
      IBlockRotator blockRotator,
      BlockDataFetcherFactory dataFetcherFactory,
      ILightDataManager lightDataManager
   ) {
      super(portal, logger, renderConfig);
      this.blockRotator = blockRotator;
      this.dataFetcherFactory = dataFetcherFactory;
      this.rotateDestToOrigin = portal.getTransformations().getRotateToOrigin();
      this.lightDataManager = lightDataManager;
      this.originWorld = portal.getOriginPos().getWorld();
      logger.fine("Origin pos: %s, Dest pos: %s", this.portalOriginPos, this.portalDestPos);
      logger.fine("Origin direction: %s, Dest Direction: %s", portal.getOriginPos().getDirection(), portal.getDestPos().getDirection());
   }

   @Override
   protected void searchFromBlock(IntVector start, List<IViewableBlockInfo> statesOutput, @Nullable IViewableBlockInfo firstBlockInfo) {
      WrappedBlockData backgroundData = this.getBackgroundData();
      int timeBetweenLightBlocks = this.renderConfig.getLightSimulationInterval();
      if (this.wrappedLightData == null) {
         this.wrappedLightData = this.lightDataManager.getLightData(this.portal);
      }

      boolean enableLightBlocks = this.wrappedLightData != null && timeBetweenLightBlocks >= 1;
      int airCount = 0;
      int[] stack = new int[Math.max(16, this.renderConfig.getTotalArrayLength())];
      stack[0] = this.getArrayMapIndex(start.subtract(this.centerPos));
      int stackPos = 0;

      while (stackPos >= 0) {
         int positionInt = stack[stackPos--];
         int relX = positionInt % this.renderConfig.getZMultip();
         int relY = Math.floorDiv(positionInt, this.renderConfig.getYMultip());
         int relZ = Math.floorDiv(positionInt - relY * this.renderConfig.getYMultip(), this.renderConfig.getZMultip());
         relX = (int)(relX - this.renderConfig.getMaxXZ());
         relY = (int)(relY - this.renderConfig.getMaxY());
         relZ = (int)(relZ - this.renderConfig.getMaxXZ());
         IntVector originPos = new IntVector(relX + this.portalOriginPos.getX(), relY + this.portalOriginPos.getY(), relZ + this.portalOriginPos.getZ());
         IntVector destRelPos = this.rotateOriginToDest.transform(relX, relY, relZ);
         IntVector destPos = destRelPos.add(this.portalDestPos);
         BlockData destData = this.dataFetcher.getData(destPos);
         if (destData == null) {
            this.logger.warning("Fetched data was null even though the request to get the data had already succeeded. This shouldn't happen!");
            return;
         }

         boolean isOccluding = destData.getMaterial().isOccluding();
         Block originBlock = originPos.getBlock(this.originWorld);
         BlockData originData = originBlock.getBlockData();
         this.handleTileEntityUpdates(originBlock, originPos, originData, destData, destPos);
         BukkitBlockInfo blockInfo = firstBlockInfo == null ? new BukkitBlockInfo(originPos, originData, destData) : (BukkitBlockInfo)firstBlockInfo;
         boolean isEdge = this.renderConfig.isOutsideBounds(relX, relY, relZ);
         this.updateRenderedData(isEdge, isOccluding, blockInfo, backgroundData, destData);
         if (firstBlockInfo == null) {
            this.nonObscuredStates.add(blockInfo);
         }

         firstBlockInfo = null;
         boolean canSkip = this.shouldSkipBlock(destData, originData, this.firstUpdate, isEdge);
         boolean isInLine = this.isInLine(destRelPos);
         if (this.alreadyReachedMap[positionInt] < 2 && !isInLine) {
            if (enableLightBlocks && destData.getMaterial().isAir() && !isEdge) {
               if (++airCount == timeBetweenLightBlocks) {
                  airCount = 0;
                  blockInfo.setRenderedDestData(this.wrappedLightData);
                  this.alreadyReachedMap[positionInt] = 2;
                  statesOutput.add(blockInfo);
               }
            } else if (!canSkip) {
               this.alreadyReachedMap[positionInt] = 2;
               statesOutput.add(blockInfo);
            }
         }

         if (!isOccluding && !isEdge) {
            if (!this.firstUpdate && stack.length - (stackPos + 1) < 5) {
               stack = Arrays.copyOf(stack, stack.length * 2);
            }

            for (int offset : this.renderConfig.getIntOffsets()) {
               int newPos = positionInt + offset;
               if (this.alreadyReachedMap[newPos] == 0) {
                  this.alreadyReachedMap[newPos] = 1;
                  stack[++stackPos] = newPos;
               }
            }
         }
      }
   }

   private void handleTileEntityUpdates(Block originBlock, IntVector originPos, BlockData originData, BlockData destData, IntVector destPos) {
      if (!this.portal.isCrossServer() && MaterialUtil.isTileEntity(destData.getMaterial())) {
         this.logger.finer("Adding tile state to map . . .");
         Block destBlock = destPos.getBlock(Objects.requireNonNull(this.portal.getDestPos().getWorld()));
         PacketContainer updatePacket = BlockDataUtil.getUpdatePacket(destBlock.getState());
         if (updatePacket != null) {
            BlockDataUtil.setTileEntityPosition(updatePacket, originPos);
            this.destTileStates.put(originPos, updatePacket);
         }
      }

      if (MaterialUtil.isTileEntity(originBlock.getType())) {
         this.logger.finer("Adding tile state to map . . .");
         PacketContainer updatePacket = BlockDataUtil.getUpdatePacket(originBlock.getState());
         if (updatePacket != null) {
            this.originTileStates.put(originPos, updatePacket);
         }
      }
   }

   private void updateRenderedData(boolean isEdge, boolean isOccluding, BukkitBlockInfo blockInfo, WrappedBlockData backgroundData, BlockData destData) {
      if (isEdge && !isOccluding) {
         blockInfo.setRenderedDestData(backgroundData);
      } else {
         blockInfo.setRenderedDestData(WrappedBlockData.createData(this.blockRotator.rotateByMatrix(this.rotateDestToOrigin, destData)));
      }
   }

   private boolean shouldSkipBlock(BlockData destData, BlockData originData, boolean firstUpdate, boolean isEdge) {
      return destData.equals(originData) && firstUpdate && !isEdge;
   }

   @Override
   protected void checkForChanges() {
      List<IViewableBlockInfo> newStates = new ArrayList<>();
      int statesLength = this.nonObscuredStates.size();

      for (int i = 0; i < statesLength; i++) {
         BukkitBlockInfo blockInfo = (BukkitBlockInfo)this.nonObscuredStates.get(i);
         IntVector originPos = blockInfo.getOriginPos();
         IntVector destPos = this.rotateOriginToDest.transform(originPos.subtract(this.portalOriginPos)).add(this.portalDestPos);
         BlockData newDestData = this.dataFetcher.getData(destPos);
         if (newDestData != null) {
            if (!newDestData.equals(blockInfo.getBaseDestData())) {
               this.logger.finer("Destination block change detected at " + destPos);
               blockInfo.setBaseDestData(newDestData);
               this.searchFromBlock(originPos, newStates, blockInfo);
            }

            if (!this.portal.isCrossServer() && MaterialUtil.isTileEntity(newDestData.getMaterial())) {
               Block destBlock = destPos.getBlock(Objects.requireNonNull(this.portal.getDestPos().getWorld()));
               PacketContainer updatePacket = BlockDataUtil.getUpdatePacket(destBlock.getState());
               if (updatePacket != null) {
                  BlockDataUtil.setTileEntityPosition(updatePacket, originPos);
                  this.destTileStates.put(originPos, updatePacket);
               }
            }

            Block originBlock = originPos.getBlock(this.originWorld);
            BlockData newOriginData = originBlock.getBlockData();
            if (MaterialUtil.isTileEntity(originBlock.getType())) {
               PacketContainer updatePacket = BlockDataUtil.getUpdatePacket(originBlock.getState());
               if (updatePacket != null) {
                  this.originTileStates.put(originPos, updatePacket);
               }
            }

            if (!newOriginData.equals(blockInfo.getBaseOriginData())) {
               blockInfo.setOriginData(newOriginData);
               if (!newOriginData.equals(newDestData) && !this.portal.getOriginPos().isInLine(originPos)) {
                  int position = this.getArrayMapIndex(originPos.subtract(this.portalOriginPos));
                  if (this.alreadyReachedMap[position] < 2) {
                     this.alreadyReachedMap[position] = 2;
                     newStates.add(blockInfo);
                  }
               }
            }
         }
      }

      this.updateTileStateMap(this.originTileStates, this.originWorld, false);
      if (!this.portal.isCrossServer()) {
         this.updateTileStateMap(this.destTileStates, this.portal.getDestPos().getWorld(), true);
      }

      if (!newStates.isEmpty()) {
         this.stateQueue.enqueueStates(newStates);
      }
   }

   private void updateTileStateMap(ConcurrentMap<IntVector, PacketContainer> map, World world, boolean isDestination) {
      for (Entry<IntVector, PacketContainer> entry : map.entrySet()) {
         IntVector position;
         if (isDestination) {
            IntVector portalRelativePos = entry.getKey().subtract(this.portalOriginPos);
            position = this.rotateOriginToDest.transform(portalRelativePos).add(this.portalDestPos);
         } else {
            position = entry.getKey();
         }

         Block block = position.getBlock(world);
         BlockState state = block.getState();
         if (!MaterialUtil.isTileEntity(state.getType())) {
            this.logger.finer("Removing tile state from map . . . %b", isDestination);
            map.remove(entry.getKey());
         }
      }
   }

   @Override
   protected void updateInternal() {
      if (this.dataFetcher == null) {
         this.dataFetcher = this.dataFetcherFactory.create(this.portal);
      }

      this.dataFetcher.update();
      if (!this.dataFetcher.isReady()) {
         this.logger.fine("Not updating portal, data was not yet been fetched");
      } else {
         super.updateInternal();
      }
   }

   @Override
   public void reset() {
      this.dataFetcher = null;
      this.wrappedLightData = null;
      super.reset();
   }
}
