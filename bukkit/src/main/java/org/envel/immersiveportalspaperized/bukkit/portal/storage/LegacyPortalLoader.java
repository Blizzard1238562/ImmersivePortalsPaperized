package org.envel.immersiveportalspaperized.bukkit.portal.storage;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

/**
 * LegacyPortalLoader.
 */
@Singleton
public class LegacyPortalLoader {
   private final IPortal.Factory portalFactory;

   @Inject
   public LegacyPortalLoader(IPortal.Factory portalFactory) {
      this.portalFactory = portalFactory;
   }

   @NotNull
   private Vector loadPortalSize(@NotNull ConfigurationSection section) {
      return new Vector(section.getInt("x"), section.getInt("y"), 0.0);
   }

   @NotNull
   private Location loadLocation(@NotNull ConfigurationSection section) {
      return new Location(
         Bukkit.getWorld(Objects.requireNonNull(section.getString("world"), "Missing world section")),
         section.getDouble("x"),
         section.getDouble("y"),
         section.getDouble("z")
      );
   }

   public IPortal loadLegacyPortal(ConfigurationSection section) {
      PortalPosition originPos = new PortalPosition(
         this.loadLocation(Objects.requireNonNull(section.getConfigurationSection("portalPosition"), "Missing origin position")),
         PortalDirection.fromStorage(Objects.requireNonNull(section.getString("portalDirection"), "Missing origin direction"))
      );
      PortalPosition destPos = new PortalPosition(
         this.loadLocation(Objects.requireNonNull(section.getConfigurationSection("destinationPosition"), "Missing destination position")),
         PortalDirection.fromStorage(Objects.requireNonNull(section.getString("destinationDirection"), "Missing destination direction"))
      );
      Vector portalSize = this.loadPortalSize(Objects.requireNonNull(section.getConfigurationSection("portalSize"), "Missing portal size"));
      boolean anchored = section.getBoolean("anchored");
      String owner = section.getString("owner");
      UUID ownerId = owner == null ? null : UUID.fromString(owner);
      return this.portalFactory.create(originPos, destPos, portalSize, anchored, UUID.randomUUID(), ownerId, null, true);
   }
}


