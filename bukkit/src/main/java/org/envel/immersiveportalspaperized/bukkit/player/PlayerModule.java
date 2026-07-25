package org.envel.immersiveportalspaperized.bukkit.player;

import org.bukkit.Bukkit;
import org.envel.immersiveportalspaperized.bukkit.player.view.IPlayerPortalView;
import org.envel.immersiveportalspaperized.bukkit.player.view.PlayerPortalView;
import org.envel.immersiveportalspaperized.bukkit.player.view.PlayerPortalViewFactory;
import org.envel.immersiveportalspaperized.bukkit.player.view.block.IPlayerBlockStates;
import org.envel.immersiveportalspaperized.bukkit.player.view.block.PlayerBlockStates;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.IPortalSelection;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.ISelectionManager;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.PortalSelection;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.SelectionManager;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

public class PlayerModule extends AbstractModule {
   @Override
   public void configure() {
      this.install(new FactoryModuleBuilder().implement(IPlayerBlockStates.class, PlayerBlockStates.class).build(IPlayerBlockStates.Factory.class));
      this.install(new FactoryModuleBuilder().implement(IPlayerData.class, PlayerData.class).build(IPlayerData.Factory.class));
      this.install(new FactoryModuleBuilder().implement(IPlayerPortalView.class, PlayerPortalView.class).build(PlayerPortalViewFactory.class));
      double blockSendUpdateDistance = Bukkit.getServer().getViewDistance() * 25;
      this.bind(double.class).annotatedWith(Names.named("blockSendUpdateDistance")).toInstance(blockSendUpdateDistance);
      this.bind(IPlayerDataManager.class).to(PlayerDataManager.class).asEagerSingleton();
      this.bind(ISelectionManager.class).to(SelectionManager.class);
      this.bind(IPortalSelection.class).to(PortalSelection.class);
   }
}
