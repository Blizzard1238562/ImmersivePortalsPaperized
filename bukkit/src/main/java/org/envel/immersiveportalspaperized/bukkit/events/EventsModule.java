package org.envel.immersiveportalspaperized.bukkit.events;

import com.google.inject.AbstractModule;
import org.envel.immersiveportalspaperized.bukkit.update.UpdateNotifyListener;

/**
 * EventsModule.
 */
public class EventsModule extends AbstractModule {
   @Override
   public void configure() {
      this.bind(IEventRegistrar.class).to(EventRegistrar.class);
      this.bind(PortalTeleportationEvents.class).asEagerSingleton();
      this.bind(SelectionEvents.class).asEagerSingleton();
      this.bind(SpawningEvents.class).asEagerSingleton();
      this.bind(UpdateNotifyListener.class).asEagerSingleton();
   }
}


