package org.envel.immersiveportalspaperized.bukkit.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.util.performance.OperationTimer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public abstract class FloodFillBlockMap implements IBlockMap {
   protected final Logger logger;
   protected final RenderConfig renderConfig;
   protected final ConcurrentHashMap<IntVector, WrapperPlayServerBlockEntityData> originTileStates = new ConcurrentHashMap<>();
   protected final ConcurrentHashMap<IntVector, WrapperPlayServerBlockEntityData> destTileStates = new ConcurrentHashMap<>();
   protected StateQueue stateQueue;
   protected List<IViewableBlockInfo> nonObscuredStates = new ArrayList<>();
   protected byte[] alreadyReachedMap;
   protected final IPortal portal;
   protected final Matrix rotateOriginToDest;
   protected final IntVector portalOriginPos;
   protected final IntVector portalDestPos;
   protected final IntVector centerPos;
   protected final PortalDirection destDirection;
   protected boolean firstUpdate;

   public FloodFillBlockMap(IPortal portal, Logger logger, RenderConfig renderConfig) {
      this.portal = portal;
      this.logger = logger;
      this.renderConfig = renderConfig;
      this.centerPos = new IntVector(portal.getOriginPos().getVector());
      this.rotateOriginToDest = portal.getTransformations().getRotateToDestination();
      this.destDirection = portal.getDestPos().getDirection();
      this.portalOriginPos = new IntVector(portal.getOriginPos().getVector());
      this.portalDestPos = this.roundBasedOnDirection(portal);
      this.reset();
   }

   private IntVector roundBasedOnDirection(IPortal portal) {
      Vector originPosVec = portal.getOriginPos().getVector();
      Vector originPosCenter = MathUtil.moveToCenterOfBlock(originPosVec);
      Vector relOriginPos = originPosCenter.clone().subtract(originPosVec);
      Vector relDestPos = portal.getTransformations().rotateToDestination(relOriginPos);
      Vector destPosCenter = portal.getDestPos().getVector().add(relDestPos);
      return new IntVector(destPosCenter);
   }

   protected boolean isInLine(IntVector relPos) {
      return this.destDirection.swapVector(relPos).getZ() == 0;
   }

   protected final int getArrayMapIndex(IntVector relPos) {
      return relPos.getX()
         + (int)this.renderConfig.getMaxXZ()
         + (relPos.getZ() + (int)this.renderConfig.getMaxXZ()) * this.renderConfig.getZMultip()
         + (relPos.getY() + (int)this.renderConfig.getMaxY()) * this.renderConfig.getYMultip();
   }

   protected abstract void searchFromBlock(IntVector start, List<IViewableBlockInfo> statesOutput, @Nullable IViewableBlockInfo firstBlockInfo);

   protected abstract void checkForChanges();

   protected final WrappedBlockState getBackgroundData() {
      return this.renderConfig.findBackgroundData(this.portal.getDestPos());
   }

   @Override
   public void update(int ticksSinceActivated) {
      if (ticksSinceActivated % this.renderConfig.getBlockUpdateInterval() == 0) {
         this.updateInternal();
      }
   }

   protected void updateInternal() {
      if (this.alreadyReachedMap == null) {
         this.alreadyReachedMap = new byte[this.renderConfig.getTotalArrayLength()];
      }

      OperationTimer timer = new OperationTimer();
      if (this.firstUpdate) {
         List<IViewableBlockInfo> initialStates = new ArrayList<>();
         this.searchFromBlock(this.centerPos, initialStates, null);
         this.stateQueue.addStatesInitially(initialStates);
      } else {
         this.checkForChanges();
      }

      this.firstUpdate = false;
      this.logger
         .fine(
            "Viewable block array update took: %.3f ms. Block count: %d. Viewable count: %d",
            timer.getTimeTakenMillis(),
            this.nonObscuredStates.size(),
            this.stateQueue.stateCount()
         );
   }

   @Override
   public void reset() {
      this.logger.finer("Clearing block array to save memory");
      this.stateQueue = new StateQueue(this.logger);
      this.nonObscuredStates = new ArrayList<>();
      this.originTileStates.clear();
      this.destTileStates.clear();
      this.firstUpdate = true;
      this.alreadyReachedMap = null;
   }

   @Override
   public List<IViewableBlockInfo> getViewableStates() {
      return this.stateQueue == null ? null : this.stateQueue.getViewableStates();
   }

   @Nullable
   @Override
   public WrapperPlayServerBlockEntityData getOriginTileEntityPacket(@NotNull IntVector position) {
      return this.originTileStates.get(position);
   }

   @Nullable
   @Override
   public WrapperPlayServerBlockEntityData getDestinationTileEntityPacket(@NotNull IntVector position) {
      return this.destTileStates.get(position);
   }
}
