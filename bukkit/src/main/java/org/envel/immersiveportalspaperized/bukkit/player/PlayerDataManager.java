package org.envel.immersiveportalspaperized.bukkit.player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.config.ProxyConfig;
import org.envel.immersiveportalspaperized.bukkit.events.IEventRegistrar;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetSelectionRequest;
import org.envel.immersiveportalspaperized.bukkit.player.view.block.PlayerBlockStates;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.IPortalSelection;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.ISelectionManager;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.requests.TeleportRequest;

/**
 * PlayerDataManager.
 */
@Singleton
public class PlayerDataManager implements IPlayerDataManager, Listener {
   private static final long LOGGED_OUT_SELECTION_CLEANUP_DELAY_TICKS = 6000L;
   private final Logger logger;
   private final IPlayerData.Factory playerDataFactory;
   private final Map<UUID, IPlayerData> players = new HashMap<>();
   private final Collection<IPlayerData> playersView = Collections.unmodifiableCollection(this.players.values());
   private final ProxyConfig proxyConfig;
   private final Map<UUID, TeleportRequest> pendingTeleportOnJoin = new HashMap<>();
   private final Map<UUID, GetSelectionRequest.ExternalSelectionInfo> pendingSelectionOnJoin = new HashMap<>();
   private final Map<UUID, ISelectionManager> loggedOutPlayerSelections = new HashMap<>();

   @Inject
   public PlayerDataManager(IEventRegistrar eventRegistrar, Logger logger, IPlayerData.Factory playerDataFactory, ProxyConfig proxyConfig) {
      this.logger = logger;
      this.playerDataFactory = playerDataFactory;
      this.proxyConfig = proxyConfig;
      this.addExistingPlayers();
      eventRegistrar.register(this);
   }

   private void addExistingPlayers() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         this.players.put(player.getUniqueId(), this.playerDataFactory.create(player));
      }
   }

   @NotNull
   @Override
   public Collection<IPlayerData> getPlayers() {
      return this.playersView;
   }

   @Nullable
   @Override
   public IPlayerData getPlayerData(@NotNull Player player) {
      return this.players.get(player.getUniqueId());
   }

   @Override
   public void onPluginDisable() {
      this.players.values().forEach(IPlayerData::onPluginDisable);
      this.players.clear();
      this.pendingTeleportOnJoin.clear();
      this.pendingSelectionOnJoin.clear();
      this.loggedOutPlayerSelections.clear();
   }

   @Override
   public void onPluginReload() {
      this.players.values().forEach(IPlayerData::onPluginDisable);
      this.players.clear();
      this.pendingTeleportOnJoin.clear();
      this.pendingSelectionOnJoin.clear();
      this.loggedOutPlayerSelections.clear();
      this.addExistingPlayers();
   }

   @Override
   public void setTeleportOnJoin(TeleportRequest request) {
      this.pendingTeleportOnJoin.put(request.getPlayerId(), request);
   }

   @Override
   public void setExternalSelectionOnLogin(UUID uniqueId, GetSelectionRequest.ExternalSelectionInfo selection) {
      Player player = Bukkit.getPlayer(uniqueId);
      if (player != null) {
         this.logger.fine("Directly setting external selection for player with ID %s", uniqueId);
         IPlayerData data = this.players.get(uniqueId);
         if (data != null) {
            data.getSelection().setExternalSelection(selection);
         }
      } else {
         this.logger.fine("Setting external selection to pending for player with ID %s", uniqueId);
         this.pendingSelectionOnJoin.put(uniqueId, selection);
      }
   }

   @Nullable
   @Override
   public IPortalSelection getDestinationSelectionWhenLoggedOut(UUID uniqueId) {
      Player player = Bukkit.getPlayer(uniqueId);
      if (player != null) {
         IPlayerData data = this.players.get(uniqueId);
         return data != null ? data.getSelection().getDestSelection() : null;
      } else {
         ISelectionManager selection = this.loggedOutPlayerSelections.get(uniqueId);
         if (selection == null) {
            if (this.proxyConfig.isWarnOnMissingSelection()) {
               this.logger
                  .warning(
                     "No selection found for player with unique ID %s. (selection check triggered by server switch, selection must be mirrored to the destination server). Is UUID forwarding disabled?",
                     uniqueId
                  );
            }

            return null;
         } else {
            return selection.getDestSelection();
         }
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      UUID playerId = event.getPlayer().getUniqueId();
      this.logger.fine("Registering player data on join for player: %s", playerId);
      IPlayerData playerData = this.playerDataFactory.create(event.getPlayer());
      this.players.put(playerId, playerData);
      TeleportRequest teleportOnJoin = this.pendingTeleportOnJoin.remove(playerId);
      if (teleportOnJoin != null) {
         this.processTeleportOnJoin(event.getPlayer(), teleportOnJoin);
      }

      ISelectionManager selectionManager = this.loggedOutPlayerSelections.remove(playerId);
      if (selectionManager != null) {
         this.logger.fine("Restoring selection on join");
         playerData.setSelection(selectionManager);
      }

      playerData.getSelection().setExternalSelection(this.pendingSelectionOnJoin.get(playerId));
   }

   private void processTeleportOnJoin(@NotNull Player player, @NotNull TeleportRequest request) {
      World world = Bukkit.getWorld(request.getDestWorldId());
      if (world == null) {
         world = Bukkit.getWorld(request.getDestWorldName());
      }

      Location destinationPosition = new Location(
         world, request.getDestX(), request.getDestY(), request.getDestZ(), request.getDestYaw(), request.getDestPitch()
      );
      Vector destinationVelocity = new Vector(request.getDestVelX(), request.getDestVelY(), request.getDestVelZ());
      player.teleportAsync(destinationPosition);
      player.setVelocity(destinationVelocity);
      player.setFlying(request.isFlying());
      player.setGliding(request.isGliding());
   }

   @EventHandler
   public void onPlayerLeave(PlayerQuitEvent event) {
      this.logger.fine("Saving selection on leave");
      UUID playerId = event.getPlayer().getUniqueId();
      IPlayerData playerData = this.players.get(playerId);
      if (playerData == null) {
         this.logger.warning("Player left with no registered data. This should not happen!");
      } else {
         this.loggedOutPlayerSelections.put(playerId, playerData.getSelection());
         SchedulerUtil.runTaskLater(() -> {
            if (Bukkit.getPlayer(playerId) == null) {
               this.loggedOutPlayerSelections.remove(playerId);
               this.logger.fine("Cleaned up expired selection for offline player %s", playerId);
            }
         }, LOGGED_OUT_SELECTION_CLEANUP_DELAY_TICKS);
         this.logger.fine("Unregistering player data on leave for player: %s", playerId);
         this.players.remove(playerId);
         playerData.onLogout();
         PlayerBlockStates.clearPlayer(playerId);
      }
   }
}


