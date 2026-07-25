package org.envel.immersiveportalspaperized.bukkit.portal.predicate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.events.IEventRegistrar;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
public class TeleportCooldownChecker implements PortalPredicate, Listener {
   private final MiscConfig miscConfig;
   private final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

   @Inject
   public TeleportCooldownChecker(MiscConfig miscConfig, IEventRegistrar eventRegistrar) {
      this.miscConfig = miscConfig;
      eventRegistrar.register(this);
   }

   @Override
   public boolean test(@NotNull ImmersivePortal portal, @NotNull Player player) {
      long cooldownMs = this.miscConfig.getTeleportCooldown() * 1000L;
      if (cooldownMs <= 0L) {
         return true;
      } else {
         long now = System.currentTimeMillis();
         Long lastTime = this.lastTeleportTime.get(player.getUniqueId());
         if (lastTime != null && now - lastTime < cooldownMs) {
            return false;
         } else {
            this.lastTeleportTime.put(player.getUniqueId(), now);
            return true;
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      this.lastTeleportTime.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
   }
}
