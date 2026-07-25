package org.envel.immersiveportalspaperized.bukkit.player.view;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.player.view.block.IPlayerBlockView;
import org.envel.immersiveportalspaperized.bukkit.player.view.entity.IPlayerEntityView;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.util.StringUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class PlayerPortalView implements IPlayerPortalView {
   private final Player player;
   private final Logger logger;
   private final RenderConfig renderConfig;
   private final IPlayerBlockView blockView;
   private final IPlayerEntityView entityView;
   private Location previousPosition = null;
   private int ticksSinceStarted = 0;

   @Inject
   public PlayerPortalView(
      @Assisted Player player, @Assisted IPortal viewedPortal, ViewFactory viewFactory, Logger logger, RenderConfig renderConfig, MiscConfig miscConfig
   ) {
      this.player = player;
      this.logger = logger;
      this.renderConfig = renderConfig;
      this.blockView = viewFactory.createBlockView(player, viewedPortal);
      if (!miscConfig.isEntitySupportEnabled()) {
         this.entityView = null;
      } else {
         this.entityView = viewFactory.createEntityView(player, viewedPortal);
      }
   }

   private boolean shouldSendPackets() {
      if (this.previousPosition == null) {
         return false;
      } else {
         Location currentPosition = this.player.getLocation();
         this.logger
            .finer(
               "Checking deactivation type of portal view, previous pos: %s, current pos: %s",
               StringUtil.locationToString(this.previousPosition),
               StringUtil.locationToString(currentPosition)
            );
         return this.previousPosition.getWorld() != currentPosition.getWorld()
            ? false
            : currentPosition.distance(this.previousPosition) < Bukkit.getViewDistance() * 16 * 2;
      }
   }

   @Override
   public void update() {
      boolean moved = this.previousPosition == null || !this.player.getLocation().toVector().equals(this.previousPosition.toVector());
      if (this.ticksSinceStarted % this.renderConfig.getBlockStateRefreshInterval() == 0) {
         this.blockView.update(true);
      } else if (moved) {
         this.blockView.update(false);
      }

      if (this.entityView != null) {
         this.entityView.update();
      }

      this.ticksSinceStarted++;
      this.previousPosition = this.player.getLocation();
   }

   @Override
   public void onDeactivate(boolean loggingOut) {
      boolean shouldSendPackets = this.shouldSendPackets() && !loggingOut;
      this.blockView.onDeactivate(shouldSendPackets);
      if (this.entityView != null) {
         this.entityView.onDeactivate(shouldSendPackets);
      }
   }
}
