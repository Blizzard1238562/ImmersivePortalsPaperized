package org.envel.immersiveportalspaperized.bukkit.player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.ImmersivePortalsPaperized;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.player.view.IPlayerPortalView;
import org.envel.immersiveportalspaperized.bukkit.player.view.PlayerPortalViewFactory;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalActivityManager;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.IPortalPredicateManager;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.ISelectionManager;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class PlayerData implements IPlayerData {
   @Getter
   private final Player player;
   @Getter
   private final YamlConfiguration permanentData;
   @Getter
   @Setter
   private ISelectionManager selection;
   private final ImmersivePortalsPaperized pl;
   private final Logger logger;
   private final IPortalManager portalManager;
   private final IPortalPredicateManager portalPredicateManager;
   private final IPortalActivityManager portalActivityManager;
   private final MiscConfig miscConfig;
   private final PlayerPortalViewFactory playerPortalViewFactory;
   private final Map<IPortal, IPlayerPortalView> portalViews = new ConcurrentHashMap<>();
   private boolean viewsFrozen;

   @Inject
   public PlayerData(
      @Assisted Player player,
      ISelectionManager selection,
      IPortalManager portalManager,
      IPortalPredicateManager portalPredicateManager,
      ImmersivePortalsPaperized pl,
      Logger logger,
      IPortalActivityManager portalActivityManager,
      PlayerPortalViewFactory playerPortalViewFactory,
      MiscConfig miscConfig
   ) {
      this.player = player;
      this.selection = selection;
      this.portalManager = portalManager;
      this.portalPredicateManager = portalPredicateManager;
      this.pl = pl;
      this.logger = logger;
      this.portalActivityManager = portalActivityManager;
      this.playerPortalViewFactory = playerPortalViewFactory;
      this.miscConfig = miscConfig;
      this.permanentData = this.loadPermanentDataYml();
   }

   @NotNull
   @Override
   public Collection<IPortal> getViewedPortals() {
      return Collections.unmodifiableCollection(this.portalViews.keySet());
   }

   private void updatePortalViews(Collection<IPortal> nowViewablePortals) {
      for (Entry<IPortal, IPlayerPortalView> entry : this.portalViews.entrySet()) {
         if (nowViewablePortals.contains(entry.getKey()) && this.player.getWorld() == entry.getKey().getOriginPos().getWorld()) {
            this.portalActivityManager.onPortalViewedThisTick(entry.getKey());
            entry.getValue().update();
         } else {
            this.logger.finer("Portal no longer being viewed by player %s", this.player.getUniqueId());
            this.setNotViewing(entry.getKey());
         }
      }
   }

   private Collection<IPortal> updateViewablePortals() {
      List<IPortal> nowViewablePortals = new ArrayList<>();

      for (IPortal portal : this.portalManager.findActivatablePortals(this.player)) {
         this.portalActivityManager.onPortalActivatedThisTick(portal);
         if (this.portalPredicateManager.isViewable(portal, this.player)) {
            nowViewablePortals.add(portal);
         }
      }

      int limit = this.miscConfig.getMaxPortalsPerPlayer();
      if (limit > 0 && nowViewablePortals.size() > limit) {
         Location playerLoc = this.player.getLocation();
         nowViewablePortals.sort(Comparator.comparingDouble(p -> p.getOriginPos().getLocation().distanceSquared(playerLoc)));
         nowViewablePortals = nowViewablePortals.subList(0, limit);
      }

      for (IPortal portalx : nowViewablePortals) {
         if (!this.portalViews.containsKey(portalx)) {
            this.setViewing(portalx);
            this.logger.finer("Portal now being viewed by player %s", this.player.getUniqueId());
         }
      }

      return nowViewablePortals;
   }

   @Override
   public void onUpdate(boolean skipRendering) {
      if (!skipRendering) {
         Collection<IPortal> nowViewablePortals = this.updateViewablePortals();
         if (!this.viewsFrozen) {
            this.updatePortalViews(nowViewablePortals);
         }
      }
   }

   private void deactivateViews(boolean loggingOut) {
      for (IPlayerPortalView view : this.portalViews.values()) {
         view.onDeactivate(loggingOut);
      }

      this.portalViews.clear();
   }

   @Override
   public void onPluginDisable() {
      this.deactivateViews(false);
   }

   @Override
   public void onLogout() {
      this.deactivateViews(true);
   }

   @Override
   public void savePermanentData() {
      File dataFolder = new File(this.pl.getDataFolder(), "playerData");
      if (!dataFolder.exists()) {
         dataFolder.mkdirs();
      }

      File permanentDataFile = new File(dataFolder, this.player.getUniqueId() + ".yml");

      try {
         if (!permanentDataFile.exists()) {
            permanentDataFile.createNewFile();
         }

         this.permanentData.save(permanentDataFile);
      } catch (IOException var4) {
         this.logger.severe("Unable to save " + this.player.getName() + "'s permanent player data! \n" + var4.getMessage());
      }
   }

   @Override
   public void freezePortalViews() {
      this.viewsFrozen = true;
   }

   private void setViewing(IPortal portal) {
      this.portalViews.put(portal, this.playerPortalViewFactory.create(this.player, portal));
   }

   private void setNotViewing(IPortal portal) {
      this.portalViews.remove(portal).onDeactivate(false);
   }

   private YamlConfiguration loadPermanentDataYml() {
      File dataFolder = new File(this.pl.getDataFolder(), "playerData");
      if (!dataFolder.exists()) {
         dataFolder.mkdirs();
      }

      File permanentDataFile = new File(dataFolder, this.player.getUniqueId() + ".yml");

      try {
         if (!permanentDataFile.exists()) {
            permanentDataFile.createNewFile();
         }

         YamlConfiguration permanentDataYml = this.createDefaultDataFile(permanentDataFile);
         permanentDataYml.save(permanentDataFile);
         return permanentDataYml;
      } catch (IOException var4) {
         this.logger.severe("Unable to load " + this.player.getName() + "'s permanent player data! Default data will be used. \n" + var4.getMessage());
         return this.createDefaultDataFile(permanentDataFile);
      }
   }

   private YamlConfiguration createDefaultDataFile(File permanentDataFile) {
      YamlConfiguration permanentDataYml = YamlConfiguration.loadConfiguration(permanentDataFile);
      permanentDataYml.addDefault("seeThroughPortal", true);
      permanentDataYml.options().copyDefaults(true);
      return permanentDataYml;
   }
}
