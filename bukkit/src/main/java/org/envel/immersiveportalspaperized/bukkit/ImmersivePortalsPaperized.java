package org.envel.immersiveportalspaperized.bukkit;

import io.foxserver.common.locale.LocaleAPI;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.block.external.IExternalBlockWatcherManager;
import org.envel.immersiveportalspaperized.bukkit.command.framework.CommandTree;
import org.envel.immersiveportalspaperized.bukkit.config.ConfigManager;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.config.ProxyConfig;
import org.envel.immersiveportalspaperized.bukkit.events.IEventRegistrar;
import org.envel.immersiveportalspaperized.bukkit.net.IPortalClient;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.portal.effects.PortalEffectsTask;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.CrossServerDestinationChecker;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.SelectionVisualizer;
import org.envel.immersiveportalspaperized.bukkit.portal.storage.IPortalStorage;
import org.envel.immersiveportalspaperized.bukkit.tasks.BlockUpdateFinisher;
import org.envel.immersiveportalspaperized.bukkit.tasks.MainUpdate;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import org.envel.immersiveportalspaperized.bukkit.util.performance.OperationTimer;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class ImmersivePortalsPaperized extends JavaPlugin {
   @Inject
   private Logger logger;
   @Inject
   private ConfigManager configManager;
   @Inject
   private CommandTree commandTree;
   @Inject
   private IPortalStorage portalStorage;
   @Inject
   private IPlayerDataManager playerDataManager;
   @Inject
   private MiscConfig miscConfig;
   @Inject
   private ProxyConfig proxyConfig;
   @Inject
   private IPortalClient portalClient;
   @Inject
   private MainUpdate mainUpdate;
   @Inject
   private BlockUpdateFinisher blockUpdateFinisher;
   @Inject
   private IPortalManager portalManager;
   @Inject
   private IEventRegistrar eventRegistrar;
   @Inject
   private API apiImplementation;
   @Inject
   private IExternalBlockWatcherManager blockWatcherManager;
   @Inject
   private CrossServerDestinationChecker crossServerDestinationChecker;
   @Inject
   private SelectionVisualizer selectionVisualizer;
   @Inject
   private PortalEffectsTask portalEffectsTask;
   @Inject
   private LocaleAPI localeApi;
   private boolean firstEnable = true;
   private boolean didEnableFail = false;

   public void onEnable() {
      if (this.getServer().getPluginManager().getPlugin("packetevents") == null) {
         this.getLogger().severe("==================================================");
         this.getLogger().severe(" ImmersivePortalsPaperized REQUIRES PACKETEVENTS TO FUNCTION!");
         this.getLogger().severe(" Please download and install PacketEvents from:");
         this.getLogger().severe(" https://www.spigotmc.org/resources/packetevents-api.80279/");
         this.getLogger().severe("==================================================");
         this.didEnableFail = true;
         this.getServer().getPluginManager().disablePlugin(this);
      } else {
         SchedulerUtil.init(this);
         OperationTimer timer = new OperationTimer();
         this.saveDefaultConfig();
         if (this.firstEnable) {
            this.startup();
            if (this.didEnableFail) {
               return;
            }

            if (this.miscConfig.isTestingCommandsEnabled()) {
               this.commandTree.registerTestCommands();
            }
         } else {
            this.reloadConfig();
            this.loadConfig();
         }

         if (this.proxyConfig.isEnabled()) {
            this.logger.fine("Proxy is enabled! Initialising connection . . .");
            this.portalClient.connect();
         }

         if (!this.firstEnable) {
            this.eventRegistrar.onPluginReload();
            this.portalManager.onReload();
         }

         this.blockUpdateFinisher.start();
         this.mainUpdate.start();
         this.portalStorage.start();
         this.selectionVisualizer.start();
         this.portalEffectsTask.loadPresets();
         this.portalEffectsTask.start();
         this.apiImplementation.onEnable();
         this.firstEnable = false;
         this.logger.fine("Startup took %.03fms", timer.getTimeTakenMillis());
      }
   }

   private boolean loadConfig() {
      try {
         this.configManager.loadValues(this.getConfig(), this);
         return true;
      } catch (RuntimeException var2) {
         this.logger.warning("Failed to reload the config file. Please check your YAML syntax!: %s: %s", var2.getClass().getName(), var2.getMessage());
         return false;
      }
   }

   private void startup() {
      try {
         Injector injector = Guice.createInjector(new MainModule(this));
         injector.injectMembers(this);
      } catch (RuntimeException e) {
         this.getLogger().log(Level.SEVERE, "A critical error occurred during plugin startup", e);
         this.didEnableFail = true;
         return;
      }

      if (!this.loadConfig()) {
         this.didEnableFail = true;
      } else {
         try {
            this.portalStorage.loadPortals();
         } catch (RuntimeException | IOException e) {
            this.getLogger().log(Level.SEVERE, "Failed to load the portals from portals.yml. Did you modify it with an incorrect format?", e);
            this.didEnableFail = true;
         }
      }
   }

   public void softReload() {
      this.mainUpdate.stop();
      this.portalStorage.stop();
      this.blockUpdateFinisher.stop();
      this.selectionVisualizer.stop();
      this.portalEffectsTask.stop();
      this.apiImplementation.onDisable();
      SchedulerUtil.cancelAll();
      this.logger.fine("Performing plugin soft-reload . . .");
      if (this.proxyConfig.isEnabled()) {
         this.portalClient.shutDown();
      }

      this.reloadConfig();
      this.localeApi.reload();
      if (this.loadConfig()) {
         this.playerDataManager.onPluginReload();
         this.portalManager.onReload();
         this.blockWatcherManager.clear();
         this.crossServerDestinationChecker.clear();
         if (this.proxyConfig.isEnabled()) {
            this.portalClient.connect();
         }

         this.eventRegistrar.onPluginReload();
         this.blockUpdateFinisher.start();
         this.mainUpdate.start();
         this.portalStorage.start();
         this.selectionVisualizer.start();
         this.portalEffectsTask.loadPresets();
         this.portalEffectsTask.start();
         this.apiImplementation.onEnable();
      }
   }

   public void onDisable() {
      if (!this.didEnableFail) {
         this.mainUpdate.stop();
         this.portalStorage.stop();
         this.blockUpdateFinisher.stop();
         this.selectionVisualizer.stop();
         this.portalEffectsTask.stop();
         SchedulerUtil.cancelAll();

         try {
            this.playerDataManager.onPluginDisable();
         } catch (RuntimeException e) {
            this.logger.severe("Error occurred while resetting player views: %s", e.getMessage());
         }

         try {
            this.portalManager.onReload();
         } catch (RuntimeException e) {
            this.logger.severe("Error occurred while resetting portals: %s", e.getMessage());
         }

         this.blockWatcherManager.clear();
         this.crossServerDestinationChecker.clear();

         try {
            this.portalStorage.savePortals();
         } catch (IOException | RuntimeException e) {
            this.logger.severe("Error occurred while saving the portals to portals.yml. Check your file permissions!: %s", e.getMessage());
         }

         if (this.portalClient.isConnectionOpen()) {
            this.portalClient.shutDown();
         }

         this.logger.fine("Goodbye!");
      }
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
      return this.commandTree.onGlobalCommand(sender, label, args);
   }

   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
      return this.commandTree.onGlobalTabComplete(sender, label, args);
   }
}
