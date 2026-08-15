package org.envel.immersiveportalspaperized.bukkit.update;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.events.IEventRegistrar;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;

/**
 * Notifies players holding {@code immersiveportalspaperized.notifyupdate} (default: op) shortly
 * after they join, if {@link UpdateChecker} has found a newer release on Modrinth. Uses the
 * pre-existing {@code outOfDate} locale key (present in every bundled language file already).
 */
public class UpdateNotifyListener implements Listener {
   private static final String NOTIFY_PERMISSION = "immersiveportalspaperized.notifyupdate";
   // Small delay so the notice doesn't get lost in join-time chat spam (resource pack prompts,
   // other plugins' welcome/MOTD messages, etc.).
   private static final long NOTIFY_DELAY_TICKS = 40L;

   private final UpdateChecker updateChecker;
   private final MessageConfig messageConfig;
   private final MiscConfig miscConfig;

   @Inject
   public UpdateNotifyListener(IEventRegistrar eventRegistrar, UpdateChecker updateChecker, MessageConfig messageConfig, MiscConfig miscConfig) {
      this.updateChecker = updateChecker;
      this.messageConfig = messageConfig;
      this.miscConfig = miscConfig;
      eventRegistrar.register(this);
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      if (!this.miscConfig.isUpdateCheckEnabled() || !this.updateChecker.isUpdateAvailable()) {
         return;
      }

      Player player = event.getPlayer();
      if (!player.hasPermission(NOTIFY_PERMISSION)) {
         return;
      }

      // Folia-safe: schedules on the player's own region thread instead of the global scheduler.
      SchedulerUtil.runForEntityLater(player, () -> this.sendNotice(player), NOTIFY_DELAY_TICKS);
   }

   private void sendNotice(Player player) {
      if (!player.isOnline()) {
         return;
      }

      String message = this.messageConfig.getChatMessage(player, "outOfDate");
      if (message.isEmpty()) {
         return;
      }

      message = message.replace("{current}", this.updateChecker.getCurrentVersion())
         .replace("{new}", String.valueOf(this.updateChecker.getLatestVersion()))
         .replace("{url}", this.updateChecker.getDownloadUrl());
      player.sendMessage(message);
   }
}
