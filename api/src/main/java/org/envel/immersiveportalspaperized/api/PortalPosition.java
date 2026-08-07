package org.envel.immersiveportalspaperized.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a position in a Minecraft world, optionally associated with a direction and server.
 * <p>
 * Supports both local positions (within the same server) and external positions (on another server
 * in a cross-server portal network). Positions are serializable via {@link #serialize()} and
 * deserializable from a {@link Map}.
 * </p>
 */
public class PortalPosition implements Serializable, ConfigurationSerializable {
   private static final long serialVersionUID = 7309245176857806033L;
   @Getter
   private final PortalDirection direction;
   private final double x;
   private final double y;
   private final double z;
   @Getter
   private UUID worldId = null;
   @Getter
   private String worldName = null;
   @Getter
   @Setter
   private String serverName = null;
   private transient Location locationCache = null;

   public PortalPosition(Vector location, PortalDirection direction, String server, String worldName) {
      this.direction = direction;
      this.x = location.getX();
      this.y = location.getY();
      this.z = location.getZ();
      this.serverName = server;
      this.worldName = worldName;
   }

   public PortalPosition(Location location, PortalDirection direction) {
      this.direction = direction;
      this.x = location.getX();
      this.y = location.getY();
      this.z = location.getZ();
      if (location.getWorld() != null) {
         this.worldName = location.getWorld().getName();
         this.worldId = location.getWorld().getUID();
      }
   }

   public PortalPosition(Map<String, Object> map) {
      Object worldIdString = map.get("worldId");
      if (worldIdString != null) {
         this.worldId = UUID.fromString((String)worldIdString);
      }

      this.worldName = (String)map.get("worldName");
      this.x = (Double)map.get("x");
      this.y = (Double)map.get("y");
      this.z = (Double)map.get("z");
      this.direction = PortalDirection.valueOf((String)map.get("direction"));
      Object configServerName = map.get("serverName");
      if (configServerName != null) {
         this.serverName = (String)configServerName;
      }
   }

   @Nullable
   public World getWorld() {
      World world = null;
      if (this.worldId != null) {
         world = Bukkit.getWorld(this.worldId);
      }

      if (world == null && this.worldName != null) {
         world = Bukkit.getWorld(this.worldName);
      }

      return world;
   }

   @NotNull
   public Location getLocation() {
      if (this.locationCache == null) {
         this.locationCache = new Location(this.getWorld(), this.x, this.y, this.z);
      }

      return this.locationCache.clone();
   }

   public boolean isInLine(IntVector vec) {
      return this.direction.swapVector(this.getVector()).getBlockZ() == this.direction.swapVector(vec).getZ();
   }

   public Vector getVector() {
      return new Vector(this.x, this.y, this.z);
   }

   public IntVector getIntVector() {
      return new IntVector(this.x, this.y, this.z);
   }

   public Block getBlock() {
      if (this.isExternal()) {
         throw new IllegalStateException("Cannot get the block of an external position");
      } else {
         return this.getLocation().getBlock();
      }
   }

   public boolean isExternal() {
      return this.serverName != null;
   }

   @NotNull
   public Map<String, Object> serialize() {
      Map<String, Object> map = new HashMap<>();
      if (this.worldId != null) {
         map.put("worldId", this.worldId.toString());
      }

      map.put("worldName", this.worldName);
      map.put("x", this.x);
      map.put("y", this.y);
      map.put("z", this.z);
      map.put("direction", this.direction.toString());
      if (this.serverName != null) {
         map.put("serverName", this.serverName);
      }

      return map;
   }

   @Override
   public String toString() {
      return String.format("x: %.02f, y: %.02f, z: %.02f, worldName: %s", this.x, this.y, this.z, this.worldName);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else {
         return !(obj instanceof PortalPosition other)
            ? false
            : other.direction == this.direction
               && other.x == this.x
               && other.y == this.y
               && other.z == this.z
               && Objects.equals(other.worldId, this.worldId)
               && Objects.equals(other.worldName, this.worldName)
               && Objects.equals(other.serverName, this.serverName);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.direction, this.x, this.y, this.z, this.worldId, this.worldName, this.serverName);
   }
}
