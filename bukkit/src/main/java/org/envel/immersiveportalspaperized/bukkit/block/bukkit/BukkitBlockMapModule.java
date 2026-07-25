package org.envel.immersiveportalspaperized.bukkit.block.bukkit;

import org.envel.immersiveportalspaperized.bukkit.block.IBlockMap;
import org.envel.immersiveportalspaperized.bukkit.block.IMultiBlockChangeManager;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class BukkitBlockMapModule extends AbstractModule {
   @Override
   protected void configure() {
      this.install(new FactoryModuleBuilder().implement(IBlockMap.class, BukkitBlockMap.class).build(IBlockMap.Factory.class));
      this.install(
         new FactoryModuleBuilder()
            .implement(IMultiBlockChangeManager.class, ModernMultiBlockChangeManager.class)
            .build(IMultiBlockChangeManager.Factory.class)
      );
   }
}
