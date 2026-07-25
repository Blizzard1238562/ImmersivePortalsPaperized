package org.envel.immersiveportalspaperized.bukkit.math;

import org.bukkit.util.Vector;

public class PlaneIntersectionChecker {
   private final Vector planeCenter;
   private final Vector planeNormal;
   private final Vector maxDev;
   private final Vector rayOrigin;

   public PlaneIntersectionChecker(Vector planeCenter, Vector planeNormal, Vector rayOrigin, Vector maxDev) {
      this.planeCenter = planeCenter;
      this.planeNormal = planeNormal;
      this.rayOrigin = rayOrigin;
      this.maxDev = maxDev;
   }

   public boolean checkIfIntersects(Vector pos) {
      Vector direction = pos.clone().subtract(this.rayOrigin).normalize();
      double denominator = this.planeNormal.dot(direction);
      if (Math.abs(denominator) > MathUtil.EPSILON) {
         Vector difference = this.planeCenter.clone().subtract(this.rayOrigin);
         double t = difference.dot(this.planeNormal) / denominator;
         if (this.rayOrigin.distance(pos) < t) {
            return false;
         }

         if (t > MathUtil.EPSILON) {
            Vector portalIntersectPoint = this.rayOrigin.clone().add(direction.multiply(t));
            Vector distCenter = portalIntersectPoint.subtract(this.planeCenter);
            return Math.abs(distCenter.getX()) <= this.maxDev.getX()
               && Math.abs(distCenter.getY()) <= this.maxDev.getY()
               && Math.abs(distCenter.getZ()) <= Math.abs(this.maxDev.getZ());
         }
      }

      return false;
   }
}
