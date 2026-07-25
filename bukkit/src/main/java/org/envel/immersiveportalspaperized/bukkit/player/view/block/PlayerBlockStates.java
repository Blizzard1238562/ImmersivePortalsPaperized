package org.envel.immersiveportalspaperized.bukkit.player.view.block;

import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.block.IMultiBlockChangeManager;
import org.envel.immersiveportalspaperized.bukkit.block.IViewableBlockInfo;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class PlayerBlockStates implements IPlayerBlockStates {
   private final Player player;
   private final IMultiBlockChangeManager.Factory multiBlockChangeManagerFactory;
   private final Logger logger;
   private final IPortal portal;
   private static final Map<UUID, Map<Vector, Map<IPortal, IViewableBlockInfo>>> globalViewedStates = new ConcurrentHashMap<>();

   @Inject
   public PlayerBlockStates(@Assisted Player player, @Assisted IPortal portal, IMultiBlockChangeManager.Factory multiBlockChangeManagerFactory, Logger logger) {
      this.player = player;
      this.portal = portal;
      this.multiBlockChangeManagerFactory = multiBlockChangeManagerFactory;
      this.logger = logger;
   }

   private Map<Vector, Map<IPortal, IViewableBlockInfo>> getPlayerStates() {
      return globalViewedStates.computeIfAbsent(this.player.getUniqueId(), k -> new ConcurrentHashMap<>());
   }

   @Override
   public void resetAndUpdate(int minChunkY, int maxChunkY) {
      Map<Vector, Map<IPortal, IViewableBlockInfo>> playerStates = this.getPlayerStates();
      if (!playerStates.isEmpty()) {
         IMultiBlockChangeManager multiBlockChangeManager = this.multiBlockChangeManagerFactory.create(this.player, minChunkY, maxChunkY);
         int resetCount = 0;

         for (Entry<Vector, Map<IPortal, IViewableBlockInfo>> entry : playerStates.entrySet()) {
            Vector pos = entry.getKey();
            Map<IPortal, IViewableBlockInfo> portalMap = entry.getValue();
            IViewableBlockInfo removedInfo = portalMap.remove(this.portal);
            if (removedInfo != null) {
               resetCount++;
               if (portalMap.isEmpty()) {
                  multiBlockChangeManager.addChangeOrigin(pos, removedInfo);
                  playerStates.remove(pos);
               } else {
                  IViewableBlockInfo nextBlock = portalMap.values().iterator().next();
                  multiBlockChangeManager.addChangeDestination(pos, nextBlock);
               }
            }
         }

         if (resetCount > 0) {
            this.logger.finest("Resetting %d blocks for portal", resetCount);
            if (SchedulerUtil.isFolia()) {
               SchedulerUtil.runForEntity(this.player, multiBlockChangeManager::sendChanges);
            } else {
               multiBlockChangeManager.sendChanges();
            }
         }

         if (playerStates.isEmpty()) {
            globalViewedStates.remove(this.player.getUniqueId());
         }
      }
   }

   @Override
   public boolean setViewable(Vector position, IViewableBlockInfo block) {
      Map<Vector, Map<IPortal, IViewableBlockInfo>> playerStates = this.getPlayerStates();
      Map<IPortal, IViewableBlockInfo> portalMap = playerStates.computeIfAbsent(position, k -> new ConcurrentHashMap<>());
      IViewableBlockInfo old = portalMap.put(this.portal, block);
      return old == null && portalMap.size() == 1;
   }

   @Override
   public boolean setNonViewable(Vector position, IViewableBlockInfo block) {
      Map<Vector, Map<IPortal, IViewableBlockInfo>> playerStates = this.getPlayerStates();
      Map<IPortal, IViewableBlockInfo> portalMap = playerStates.get(position);
      if (portalMap == null) {
         return false;
      } else {
         boolean removed = portalMap.remove(this.portal, block);
         if (removed && portalMap.isEmpty()) {
            playerStates.remove(position);
            return true;
         } else {
            return false;
         }
      }
   }

   public static void clearPlayer(UUID uuid) {
      globalViewedStates.remove(uuid);
   }
}
