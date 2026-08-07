package org.envel.immersiveportalspaperized.bukkit;

import io.foxserver.common.locale.LocaleAPI;
import java.lang.reflect.Constructor;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.block.BlockModule;
import org.envel.immersiveportalspaperized.bukkit.block.rotation.IBlockRotator;
import org.envel.immersiveportalspaperized.bukkit.block.rotation.ModernBlockRotator;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkloading.IChunkLoader;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkloading.ModernChunkLoader;
import org.envel.immersiveportalspaperized.bukkit.chunk.generation.IChunkGenerationChecker;
import org.envel.immersiveportalspaperized.bukkit.chunk.generation.ModernChunkGenerationChecker;
import org.envel.immersiveportalspaperized.bukkit.command.CommandsModule;
import org.envel.immersiveportalspaperized.bukkit.economy.EconomyManager;
import org.envel.immersiveportalspaperized.bukkit.entity.EntityModule;
import org.envel.immersiveportalspaperized.bukkit.events.EventsModule;
import org.envel.immersiveportalspaperized.bukkit.gui.PortalAdminGUI;
import org.envel.immersiveportalspaperized.bukkit.net.NetworkModule;
import org.envel.immersiveportalspaperized.bukkit.player.PlayerModule;
import org.envel.immersiveportalspaperized.bukkit.portal.PortalModule;
import org.envel.immersiveportalspaperized.bukkit.portal.effects.PortalEffectsTask;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.SelectionVisualizer;
import org.envel.immersiveportalspaperized.bukkit.tasks.BlockUpdateFinisher;
import org.envel.immersiveportalspaperized.bukkit.tasks.ThreadedBlockUpdateFinisher;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.logging.OverrideLogger;
import org.envel.immersiveportalspaperized.shared.util.ReflectionUtil;

/**
 * MainModule.
 */
public class MainModule extends AbstractModule {
   private final ImmersivePortalsPaperized pl;

   public MainModule(ImmersivePortalsPaperized pl) {
      this.pl = pl;
   }

   @Override
   protected void configure() {
      this.bind(JavaPlugin.class).toInstance(this.pl);
      this.bind(ImmersivePortalsPaperized.class).toInstance(this.pl);
      this.bind(Logger.class).toInstance(new OverrideLogger(this.pl.getLogger()));
      this.bind(IChunkLoader.class).to(ModernChunkLoader.class);
      this.bind(IBlockRotator.class).to(ModernBlockRotator.class);
      this.bind(IChunkGenerationChecker.class).to(ModernChunkGenerationChecker.class);
      this.bind(BlockUpdateFinisher.class).to(ThreadedBlockUpdateFinisher.class);
      this.bind(SelectionVisualizer.class).asEagerSingleton();
      this.bind(PortalEffectsTask.class).asEagerSingleton();
      this.bind(PortalAdminGUI.class).asEagerSingleton();
      this.bind(EconomyManager.class).asEagerSingleton();
      LocaleAPI localeApi = new LocaleAPI(this.pl, "en_US", true);
      localeApi.load();
      this.pl.getServer().getPluginManager().registerEvents(localeApi, this.pl);
      this.bind(LocaleAPI.class).toInstance(localeApi);
      this.install(new EventsModule());
      this.install(new CommandsModule());
      this.install(new PortalModule());
      this.install(new BlockModule(false));
      this.install(new NetworkModule());
      this.install(new PlayerModule());
      this.install(new EntityModule(false));
   }

   private Module createNmsModule() {
      Class<?> nmsModuleClass = ReflectionUtil.findClass("org.envel.immersiveportalspaperized.bukkit.nms.direct.NmsOptimisationModule");
      Constructor<?> nmsModuleCtor = ReflectionUtil.findConstructor(nmsModuleClass);
      return (Module)ReflectionUtil.invokeConstructor(nmsModuleCtor);
   }
}


