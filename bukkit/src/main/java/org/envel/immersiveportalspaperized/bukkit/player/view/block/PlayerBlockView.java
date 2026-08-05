package org.envel.immersiveportalspaperized.bukkit.player.view.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.bukkit.block.IBlockMap;
import org.envel.immersiveportalspaperized.bukkit.block.IMultiBlockChangeManager;
import org.envel.immersiveportalspaperized.bukkit.block.IViewableBlockInfo;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.math.PlaneIntersectionChecker;
import org.envel.immersiveportalspaperized.bukkit.nms.PacketUtil;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.tasks.BlockUpdateFinisher;
import org.envel.immersiveportalspaperized.bukkit.util.HeightUtil;
import org.envel.immersiveportalspaperized.bukkit.util.MaterialUtil;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class PlayerBlockView implements IPlayerBlockView {
   private final Player player;
   private final IPortal portal;
   private final IMultiBlockChangeManager.Factory multiBlockChangeManagerFactory;
   private final IPlayerBlockStates blockStates;
   private final ReentrantLock statesLock = new ReentrantLock(true);
   private final Logger logger;
   private final BlockUpdateFinisher updateFinisher;
   private final boolean shouldHidePortalBlocks;
   private final int minChunkY;
   private final int maxChunkY;
   private volatile Vector playerPosition;
   private volatile boolean didDeactivate = false;

   @Inject
   public PlayerBlockView(
      @Assisted Player player,
      @Assisted IPortal portal,
      IMultiBlockChangeManager.Factory multiBlockChangeManagerFactory,
      IPlayerBlockStates.Factory blockStatesFactory,
      Logger logger,
      BlockUpdateFinisher updateFinisher,
      RenderConfig renderConfig
   ) {
      this.player = player;
      this.portal = portal;
      this.multiBlockChangeManagerFactory = multiBlockChangeManagerFactory;
      this.blockStates = blockStatesFactory.create(player, portal);
      this.logger = logger;
      this.updateFinisher = updateFinisher;
      this.shouldHidePortalBlocks = portal.isNetherPortal() && renderConfig.isPortalBlocksHidden();
      World viewWorld = player.getWorld();
      this.minChunkY = HeightUtil.getMinHeight(viewWorld) >> 4;
      this.maxChunkY = HeightUtil.getMaxHeight(viewWorld) >> 4;
   }

   @Override
   public void update(boolean refresh) {
      this.playerPosition = this.player.getEyeLocation().toVector();
      this.updateFinisher.scheduleUpdate(this, refresh);
      if (refresh && this.shouldHidePortalBlocks) {
         this.setPortalBlocks(SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(Material.AIR)));
      }
   }

   @Override
   public void finishReset() {
      this.statesLock.lock();

      try {
         this.blockStates.resetAndUpdate(this.minChunkY, this.maxChunkY);
      } finally {
         this.statesLock.unlock();
      }
   }

   @Override
   public void onDeactivate(boolean shouldResetStates) {
      this.didDeactivate = true;
      this.logger.finer("Player block view deactivating. Should reset states: %b", shouldResetStates);
      if (shouldResetStates) {
         if (this.shouldHidePortalBlocks && this.portal.isRegistered()) {
            this.setPortalBlocks(this.getPortalBlockData());
         }

         if (this.statesLock.tryLock()) {
            this.logger.finest("Resetting immediately!");

            try {
               this.blockStates.resetAndUpdate(this.minChunkY, this.maxChunkY);
            } finally {
               this.statesLock.unlock();
            }
         } else {
            this.logger.finest("Scheduling reset");
            this.updateFinisher.scheduleReset(this);
         }
      }
   }

   public void finishUpdate(boolean refresh) {
      if (!this.didDeactivate) {
         if (refresh) {
            this.logger.finest("Refreshing already sent blocks!");
         }

         this.statesLock.lock();

         try {
            IMultiBlockChangeManager multiBlockChangeManager = this.multiBlockChangeManagerFactory.create(this.player, this.minChunkY, this.maxChunkY);
            List<WrapperPlayServerBlockEntityData> queuedTileEntityUpdates = new ArrayList<>();
            PlaneIntersectionChecker intersectionChecker = this.portal.getTransformations().createIntersectionChecker(this.playerPosition);
            IBlockMap viewableBlockArray = this.portal.getViewableBlocks();
            List<IViewableBlockInfo> viewableStates = viewableBlockArray.getViewableStates();
            if (viewableStates == null) {
               return;
            }

            for (IViewableBlockInfo blockInfo : viewableStates) {
               Vector position = blockInfo.getOriginPos().getCenterPos();
               boolean visible = intersectionChecker.checkIfIntersects(position);
               if (visible) {
                  if (this.blockStates.setViewable(position, blockInfo) || refresh) {
                     multiBlockChangeManager.addChangeDestination(position, blockInfo);
                     WrapperPlayServerBlockEntityData nbtUpdatePacket = viewableBlockArray.getDestinationTileEntityPacket(blockInfo.getOriginPos());
                     if (nbtUpdatePacket != null) {
                        queuedTileEntityUpdates.add(nbtUpdatePacket);
                        this.logger.fine("Queueing tile state update at destination");
                     }
                  }
               } else if (this.blockStates.setNonViewable(position, blockInfo) || refresh) {
                  multiBlockChangeManager.addChangeOrigin(position, blockInfo);
                  WrapperPlayServerBlockEntityData nbtUpdatePacket = viewableBlockArray.getOriginTileEntityPacket(blockInfo.getOriginPos());
                  if (nbtUpdatePacket != null) {
                     queuedTileEntityUpdates.add(nbtUpdatePacket);
                     this.logger.fine("Queueing tile state update at origin");
                  }
               }
            }

            if (SchedulerUtil.isFolia()) {
               SchedulerUtil.runForEntity(this.player, () -> {
                  multiBlockChangeManager.sendChanges();

                  for (WrapperPlayServerBlockEntityData packetx : queuedTileEntityUpdates) {
                     PacketUtil.sendPacket(this.player, packetx);
                  }
               });
            } else {
               multiBlockChangeManager.sendChanges();

               for (WrapperPlayServerBlockEntityData packet : queuedTileEntityUpdates) {
                  PacketUtil.sendPacket(this.player, packet);
               }
            }
         } finally {
            this.statesLock.unlock();
         }
      }
   }

   private WrappedBlockState getPortalBlockData() {
      PortalDirection portalDirection = this.portal.getOriginPos().getDirection();
      if (!(Bukkit.createBlockData(MaterialUtil.PORTAL_MATERIAL) instanceof Orientable orientable)) {
         return SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(MaterialUtil.PORTAL_MATERIAL));
      } else {
         if (portalDirection != PortalDirection.EAST && portalDirection != PortalDirection.WEST) {
            if (portalDirection != PortalDirection.NORTH && portalDirection != PortalDirection.SOUTH) {
               throw new IllegalStateException("Tried to get portal block data of a horizontal portal");
            }

            orientable.setAxis(Axis.X);
         } else {
            orientable.setAxis(Axis.Z);
         }

         return SpigotConversionUtil.fromBukkitBlockData(orientable);
      }
   }

   private void setPortalBlocks(WrappedBlockState data) {
      Vector portalPos = this.portal.getOriginPos().getVector();
      Vector portalSize = this.portal.getSize();
      PortalDirection portalDirection = this.portal.getOriginPos().getDirection();
      portalPos.subtract(portalDirection.swapVector(portalSize).multiply(0.5));
      IMultiBlockChangeManager multiBlockChangeManager = this.multiBlockChangeManagerFactory.create(this.player, this.minChunkY, this.maxChunkY);

      for (int x = 0; x < portalSize.getX(); x++) {
         for (int y = 0; y < portalSize.getY(); y++) {
            Vector relativePos = portalDirection.swapVector(new Vector(x, y, 0.0));
            Vector blockPos = portalPos.clone().add(relativePos);
            multiBlockChangeManager.addChange(blockPos, data);
         }
      }

      multiBlockChangeManager.sendChanges();
   }
}
