package org.envel.immersiveportalspaperized.bukkit.block;

import org.envel.immersiveportalspaperized.bukkit.block.bukkit.BukkitBlockMapModule;
import org.envel.immersiveportalspaperized.bukkit.block.external.BlockChangeWatcher;
import org.envel.immersiveportalspaperized.bukkit.block.external.ExternalBlockWatcherManager;
import org.envel.immersiveportalspaperized.bukkit.block.external.IBlockChangeWatcher;
import org.envel.immersiveportalspaperized.bukkit.block.external.IExternalBlockWatcherManager;
import org.envel.immersiveportalspaperized.bukkit.block.lighting.DummyLightDataManager;
import org.envel.immersiveportalspaperized.bukkit.block.lighting.ILightDataManager;
import org.envel.immersiveportalspaperized.bukkit.block.lighting.LightDataManger;
import org.envel.immersiveportalspaperized.bukkit.player.view.ViewFactory;
import org.envel.immersiveportalspaperized.bukkit.player.view.block.IPlayerBlockView;
import org.envel.immersiveportalspaperized.bukkit.player.view.block.PlayerBlockView;
import org.envel.immersiveportalspaperized.bukkit.player.view.entity.IPlayerEntityView;
import org.envel.immersiveportalspaperized.bukkit.player.view.entity.PlayerEntityView;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class BlockModule extends AbstractModule {
   private final boolean usingNms;

   public BlockModule(boolean useNms) {
      this.usingNms = useNms;
   }

   @Override
   public void configure() {
      this.install(new FactoryModuleBuilder().implement(IBlockChangeWatcher.class, BlockChangeWatcher.class).build(IBlockChangeWatcher.Factory.class));
      this.install(
         new FactoryModuleBuilder()
            .implement(IPlayerBlockView.class, PlayerBlockView.class)
            .implement(IPlayerEntityView.class, PlayerEntityView.class)
            .build(ViewFactory.class)
      );
      this.bind(IExternalBlockWatcherManager.class).to(ExternalBlockWatcherManager.class);

      try {
         Class.forName("org.bukkit.block.data.type.Light");
         this.bind(ILightDataManager.class).to(LightDataManger.class);
      } catch (ClassNotFoundException var2) {
         this.bind(ILightDataManager.class).to(DummyLightDataManager.class);
      }

      if (!this.usingNms) {
         this.install(new BukkitBlockMapModule());
      }
   }
}
