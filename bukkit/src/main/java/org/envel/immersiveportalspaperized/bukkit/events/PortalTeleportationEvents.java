package org.envel.immersiveportalspaperized.bukkit.events;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.config.PortalSpawnConfig;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;

public class PortalTeleportationEvents implements Listener {
   private final PortalSpawnConfig spawnConfig;
   private final IPortalManager portalManager;
   private final MessageConfig messageConfig;

   @Inject
   public PortalTeleportationEvents(IEventRegistrar eventRegistrar, IPortalManager portalManager, PortalSpawnConfig spawnConfig, MessageConfig messageConfig) {
      this.portalManager = portalManager;
      this.messageConfig = messageConfig;
      this.spawnConfig = spawnConfig;
      eventRegistrar.register(this);
   }

   private boolean isPluginNetherPortal(@NotNull Entity entity) {
      Vector maxPortalSize = this.spawnConfig.getMaxPortalSize();
      double portalExistenceRadius = Math.max(maxPortalSize.getX(), maxPortalSize.getY()) + 2.0;
      IPortal portal = this.portalManager.findClosestPortal(entity.getLocation(), portalExistenceRadius);
      return portal != null && portal.isNetherPortal();
   }

   @EventHandler
   public void onEntityPortal(EntityPortalEvent event) {
      if (this.isPluginNetherPortal(event.getEntity())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onPlayerPortal(PlayerPortalEvent event) {
      boolean isNetherPortal = event.getCause() == TeleportCause.NETHER_PORTAL;
      if (this.isPluginNetherPortal(event.getPlayer()) && isNetherPortal) {
         event.setCancelled(true);
      } else if (isNetherPortal) {
         if (this.spawnConfig.isWorldDisabled(event.getFrom().getWorld()) || this.spawnConfig.isWorldDisabled(event.getTo().getWorld())) {
            return;
         }

         String warning = this.messageConfig.getWarningMessage(event.getPlayer(), "vanillaPortal");
         if (!warning.isEmpty()) {
            event.getPlayer().sendMessage(warning);
         }
      }
   }
}
