package org.envel.immersiveportalspaperized.bukkit.events;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * EventRegistrar.
 */
@Singleton
public class EventRegistrar implements IEventRegistrar {
   private final JavaPlugin pl;
   private final Set<Listener> allRegisteredListeners = new HashSet<>();
   private final Logger logger;

   @Inject
   public EventRegistrar(JavaPlugin pl, Logger logger) {
      this.pl = pl;
      this.logger = logger;
   }

   @Override
   public void register(@NotNull Listener listener) {
      this.pl.getServer().getPluginManager().registerEvents(listener, this.pl);
      this.allRegisteredListeners.add(listener);
   }

   @Override
   public void onPluginReload() {
      this.logger.fine("Re-registering events . . .");

      for (Listener listener : this.allRegisteredListeners) {
         HandlerList.unregisterAll(listener);
         this.pl.getServer().getPluginManager().registerEvents(listener, this.pl);
      }
   }
}


