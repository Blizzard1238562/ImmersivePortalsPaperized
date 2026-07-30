package org.envel.immersiveportalspaperized.bukkit.math;

import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class MathUtil {
   public static final double EPSILON = 1.0E-4;
   private static final double TWO_PI = Math.PI * 2;

   public static Vector round(Vector vec) {
      return new Vector((float)Math.round(vec.getX()), (float)Math.round(vec.getY()), (float)Math.round(vec.getZ()));
   }

   public static Vector abs(Vector vec) {
      return new Vector(Math.abs(vec.getX()), Math.abs(vec.getY()), Math.abs(vec.getZ()));
   }

   public static Vector floor(Vector vec) {
      return new Vector(Math.floor(vec.getX()), Math.floor(vec.getY()), Math.floor(vec.getZ()));
   }

   public static Location floor(Location loc) {
      return floor(loc.toVector()).toLocation(loc.getWorld());
   }

   public static Vector ceil(Vector vec) {
      return new Vector(Math.ceil(vec.getX()), Math.ceil(vec.getY()), Math.ceil(vec.getZ()));
   }

   public static boolean greaterThanEq(Vector a, Vector b) {
      return a.getX() >= b.getX() && a.getY() >= b.getY() && a.getZ() >= b.getZ();
   }

   public static boolean lessThanEq(Vector a, Vector b) {
      return a.getX() <= b.getX() && a.getY() <= b.getY() && a.getZ() <= b.getZ();
   }

   public static Vector moveToCenterOfBlock(Vector vec) {
      return new Vector(Math.floor(vec.getX()) + 0.5, Math.floor(vec.getY()) + 0.5, Math.floor(vec.getZ()) + 0.5);
   }

   public static Location moveToCenterOfBlock(Location loc) {
      return new Location(loc.getWorld(), Math.floor(loc.getX()) + 0.5, Math.floor(loc.getY()) + 0.5, Math.floor(loc.getZ()) + 0.5);
   }

   public static Vector min(Vector a, Vector b) {
      return new Vector(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
   }

   public static Location min(Location a, Location b) {
      return min(a.toVector(), b.toVector()).toLocation(a.getWorld());
   }

   public static Vector max(Vector a, Vector b) {
      return new Vector(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
   }

   public static Location max(Location a, Location b) {
      return max(a.toVector(), b.toVector()).toLocation(a.getWorld());
   }

   public static Vector getDirection(float yaw, float pitch) {
      float rotX = (float)Math.toRadians(yaw);
      float rotY = (float)Math.toRadians(pitch);
      double xz = Math.cos(rotY);
      return new Vector(-xz * Math.sin(rotX), -Math.sin(rotY), xz * Math.cos(rotX));
   }

   public static Pair<Float, Float> getYawAndPitch(Vector dir) {
      if (dir.getX() == 0.0 && dir.getZ() == 0.0) {
         return new Pair(0.0F, dir.getY() > 0.0 ? -90.0F : 90.0F);
      } else {
         double theta = Math.atan2(-dir.getX(), dir.getZ());
         float yaw = (float)Math.toDegrees((theta + (Math.PI * 2)) % (Math.PI * 2));
         double xz = Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ());
         float pitch = (float)Math.toDegrees(Math.atan(-dir.getY() / xz));
         return new Pair(yaw, pitch);
      }
   }
}
