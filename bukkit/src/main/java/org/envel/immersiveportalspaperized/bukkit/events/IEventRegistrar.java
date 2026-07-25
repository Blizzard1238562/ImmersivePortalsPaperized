package org.envel.immersiveportalspaperized.bukkit.events;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public interface IEventRegistrar {
   void register(@NotNull Listener listener);

   void onPluginReload();
}
