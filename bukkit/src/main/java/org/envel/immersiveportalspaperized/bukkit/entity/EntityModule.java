package org.envel.immersiveportalspaperized.bukkit.entity;

import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityPacketManipulator;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityTracker;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityTrackingManager;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EventEntityTrackingManager;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.IEntityPacketManipulator;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.IEntityTracker;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.NoUpdateEntityTrackingManager;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * EntityModule.
 */
public class EntityModule extends AbstractModule {
   private final boolean usingNms;

   public EntityModule(boolean usingNms) {
      this.usingNms = usingNms;
   }

   @Override
   public void configure() {
      if (!this.usingNms) {
         this.install(new FactoryModuleBuilder().implement(IEntityTracker.class, EntityTracker.class).build(IEntityTracker.Factory.class));
         this.bind(IEntityFinder.class).to(BukkitEntityFinder.class);
      }

      this.bind(IEntityPacketManipulator.class).to(EntityPacketManipulator.class);
      this.bind(EntityTrackingManager.class).to(this.usingNms ? NoUpdateEntityTrackingManager.class : EventEntityTrackingManager.class);
   }
}


