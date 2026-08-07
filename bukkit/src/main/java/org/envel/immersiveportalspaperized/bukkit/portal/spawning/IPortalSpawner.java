package org.envel.immersiveportalspaperized.bukkit.portal.spawning;

import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * IPortalSpawner.
 */
public interface IPortalSpawner {
   boolean findAndSpawnDestination(@NotNull Location originPosition, @NotNull Vector originSize, Consumer<PortalSpawnPosition> onFinish);
}


