package org.envel.immersiveportalspaperized.bukkit.events;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerData;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.IPortalSelection;
import com.google.inject.Inject;

public class SelectionEvents implements Listener {
   private final IPlayerDataManager playerDataManager;
   private final MessageConfig messageConfig;

   @Inject
   public SelectionEvents(IEventRegistrar eventRegistrar, IPlayerDataManager playerDataManager, MessageConfig messageConfig) {
      this.playerDataManager = playerDataManager;
      this.messageConfig = messageConfig;
      eventRegistrar.register(this);
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      ItemStack item = event.getItem();
      if (item != null && this.messageConfig.isPortalWand(item)) {
         Action action = event.getAction();
         if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (player.hasPermission("immersiveportalspaperized.wand")) {
               event.setCancelled(true);
               Location blockPos = Objects.requireNonNull(
                     event.getClickedBlock(), "Clicked block was null despite being a block event, this should never happen"
                  )
                  .getLocation();
               IPlayerData playerData = Objects.requireNonNull(this.playerDataManager.getPlayerData(player), "Player in event had no registered player data");
               playerData.getSelection().recordActivity();
               IPortalSelection selection = playerData.getSelection().getCurrentlySelecting();
               if (action == Action.LEFT_CLICK_BLOCK) {
                  selection.setPositionA(blockPos);
                  player.sendMessage(this.messageConfig.getChatMessage(player, "setPosA"));
               } else {
                  selection.setPositionB(blockPos);
                  player.sendMessage(this.messageConfig.getChatMessage(player, "setPosB"));
               }
            }
         }
      }
   }
}
