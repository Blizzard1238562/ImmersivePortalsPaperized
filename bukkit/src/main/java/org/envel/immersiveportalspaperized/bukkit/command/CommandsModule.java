package org.envel.immersiveportalspaperized.bukkit.command;

import com.google.inject.AbstractModule;

/**
 * CommandsModule.
 */
public class CommandsModule extends AbstractModule {
   @Override
   public void configure() {
      this.bind(MainCommands.class).asEagerSingleton();
      this.bind(CustomPortalCommands.class).asEagerSingleton();
   }
}


