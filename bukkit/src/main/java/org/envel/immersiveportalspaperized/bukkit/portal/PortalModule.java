package org.envel.immersiveportalspaperized.bukkit.portal;

import org.envel.immersiveportalspaperized.bukkit.entity.IPortalEntityManager;
import org.envel.immersiveportalspaperized.bukkit.entity.PortalEntityManager;
import org.envel.immersiveportalspaperized.bukkit.math.PortalTransformationsFactory;
import org.envel.immersiveportalspaperized.bukkit.portal.blend.DimensionBlendManager;
import org.envel.immersiveportalspaperized.bukkit.portal.blend.IDimensionBlendManager;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.IPortalPredicateManager;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.PortalPredicateManager;
import org.envel.immersiveportalspaperized.bukkit.portal.spawning.IPortalSpawner;
import org.envel.immersiveportalspaperized.bukkit.portal.spawning.PortalSpawner;
import org.envel.immersiveportalspaperized.bukkit.portal.storage.IPortalStorage;
import org.envel.immersiveportalspaperized.bukkit.portal.storage.YamlPortalStorage;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class PortalModule extends AbstractModule {
   @Override
   public void configure() {
      this.install(new FactoryModuleBuilder().implement(IPortal.class, Portal.class).build(IPortal.Factory.class));
      this.install(new FactoryModuleBuilder().implement(IPortalEntityManager.class, PortalEntityManager.class).build(IPortalEntityManager.Factory.class));
      this.install(new FactoryModuleBuilder().build(PortalTransformationsFactory.class));
      this.bind(IPortalPredicateManager.class).to(PortalPredicateManager.class);
      this.bind(IPortalStorage.class).to(YamlPortalStorage.class);
      this.bind(IPortalManager.class).to(PortalManager.class);
      this.bind(IPortalActivityManager.class).to(PortalActivityManager.class);
      this.bind(IPortalSpawner.class).to(PortalSpawner.class);
      this.bind(IDimensionBlendManager.class).to(DimensionBlendManager.class);
      this.requestStaticInjection(new Class[]{Portal.class});
   }
}
