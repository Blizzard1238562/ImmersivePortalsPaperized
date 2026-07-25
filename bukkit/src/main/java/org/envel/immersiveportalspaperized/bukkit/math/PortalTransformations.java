package org.envel.immersiveportalspaperized.bukkit.math;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class PortalTransformations {
   private final PortalPosition originPos;
   private final PortalPosition destPos;
   private final RenderConfig renderConfig;
   private final Vector portalSize;
   @Getter
   private final Matrix originToDestination;
   @Getter
   private Matrix rotateToDestination;
   @Getter
   private final Matrix destinationToOrigin;
   @Getter
   private Matrix rotateToOrigin;
   private final World originWorld;
   private final World destinationWorld;

   @Inject
   public PortalTransformations(@Assisted IPortal portal, RenderConfig renderConfig) {
      this.originPos = portal.getOriginPos();
      this.destPos = portal.getDestPos();
      this.portalSize = portal.getSize();
      this.renderConfig = renderConfig;
      this.rotateToDestination = Matrix.makeRotation(this.originPos.getDirection(), this.destPos.getDirection());
      this.rotateToOrigin = Matrix.makeRotation(this.destPos.getDirection(), this.originPos.getDirection());
      if ("dinnerbone".equalsIgnoreCase(portal.getName())) {
         this.applyDinnerbone();
      }

      this.originToDestination = Matrix.makeTranslation(this.destPos.getVector())
         .multiply(this.rotateToDestination)
         .multiply(Matrix.makeTranslation(this.originPos.getVector().multiply(-1.0)));
      this.destinationToOrigin = Matrix.makeTranslation(this.originPos.getVector())
         .multiply(this.rotateToOrigin)
         .multiply(Matrix.makeTranslation(this.destPos.getVector().multiply(-1.0)));
      this.originWorld = this.originPos.getWorld();
      this.destinationWorld = this.destPos.getWorld();
   }

   private void applyDinnerbone() {
      PortalDirection originDirection = this.originPos.getDirection();
      PortalDirection destinationDirection = this.destPos.getDirection();
      if (!originDirection.isHorizontal() && !destinationDirection.isHorizontal()) {
         Vector axis = destinationDirection.toVector();
         this.rotateToDestination = this.rotateToDestination.multiply(Matrix.makeRotation(axis, Math.PI));
         this.rotateToOrigin = this.rotateToOrigin.multiply(Matrix.makeRotation(axis, -Math.PI));
      }
   }

   public Location moveToDestination(Location loc) {
      Location result = this.originToDestination.transform(loc.toVector()).toLocation(this.destinationWorld);
      result.setDirection(this.rotateToDestination.transform(loc.getDirection()));
      return result;
   }

   public IntVector moveToOrigin(IntVector vec) {
      return this.destinationToOrigin.transform(vec);
   }

   public IntVector moveToDestination(IntVector vec) {
      return this.originToDestination.transform(vec);
   }

   public Location moveToOrigin(Location loc) {
      Location result = this.destinationToOrigin.transform(loc.toVector()).toLocation(this.originWorld);
      result.setDirection(this.rotateToOrigin.transform(loc.getDirection()));
      return result;
   }

   public Vector rotateToDestination(Vector vec) {
      return this.rotateToDestination.transform(vec);
   }

   public Vector rotateToOrigin(Vector vec) {
      return this.rotateToOrigin.transform(vec);
   }

   public PlaneIntersectionChecker createIntersectionChecker(Vector rayOrigin) {
      Vector planeSize = this.portalSize.clone().multiply(0.5);
      planeSize = this.originPos.getDirection().swapVector(planeSize);
      Vector collisionBoxOffset = this.originPos.getDirection().swapVector(this.renderConfig.getCollisionBox());
      planeSize.add(collisionBoxOffset);
      return new PlaneIntersectionChecker(this.originPos.getVector(), this.originPos.getDirection().toVector(), rayOrigin, planeSize);
   }
}
