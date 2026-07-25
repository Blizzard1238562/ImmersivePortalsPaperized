package org.envel.immersiveportalspaperized.bukkit.portal.selection;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class PortalSelection implements IPortalSelection {
   private Logger logger;
   private Location enteredPosA;
   private Location enteredPosB;
   private Location posA;
   private Location posB;
   private Location portalLocation;
   private PortalDirection portalDirection;
   @Getter
   private Vector portalSize;
   @Getter
   private boolean isValid;

   @Inject
   public PortalSelection(Logger logger) {
      this.logger = logger;
   }

   @Override
   public void setPositionA(@NotNull Location posA) {
      this.enteredPosA = posA;
      this.isValid = this.calculatePortalPosition();
   }

   @Override
   public void setPositionB(@NotNull Location posB) {
      this.enteredPosB = posB;
      this.isValid = this.calculatePortalPosition();
   }

   @Nullable
   @Override
   public PortalPosition getPortalPosition() {
      return this.portalLocation != null && this.portalDirection != null ? new PortalPosition(this.portalLocation, this.portalDirection) : null;
   }

   @Override
   public void invertDirection() {
      this.portalDirection = this.portalDirection.getOpposite();
   }

   private boolean calculatePortalPosition() {
      if (this.enteredPosA != null && this.enteredPosB != null) {
         this.alignCoordinates();
         if (this.posA.getWorld() != this.posB.getWorld()) {
            this.logger.fine("Portal selection was two different worlds, aborting");
            return false;
         } else {
            this.findDirection();
            if (this.portalDirection == null) {
               this.logger.fine("Portal selection was not in line with a valid portal plane, aborting");
               return false;
            } else {
               this.portalSize = this.findSize();
               if (this.portalSize.getX() >= 1.0 && this.portalSize.getY() >= 1.0) {
                  this.findPortalLocation();
                  this.logger
                     .fine(
                        "Successfully found selected portal position at location %s with direction %s and size %s",
                        this.portalLocation,
                        this.portalDirection,
                        this.portalSize
                     );
                  return true;
               } else {
                  this.logger.fine("Portal size (%s), was not large enough, aborting", this.portalSize);
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   private void findDirection() {
      if (this.posA.getZ() == this.posB.getZ()) {
         this.portalDirection = PortalDirection.NORTH;
      } else if (this.posA.getX() == this.posB.getX()) {
         this.portalDirection = PortalDirection.EAST;
      } else if (this.posA.getY() == this.posB.getY()) {
         this.portalDirection = PortalDirection.UP;
      } else {
         this.portalDirection = null;
      }
   }

   private Vector findSize() {
      this.portalSize = this.portalDirection.swapVector(this.posB.clone().subtract(this.posA).toVector());
      return this.portalSize.subtract(new Vector(1.0, 1.0, 0.0));
   }

   private void findPortalLocation() {
      this.portalLocation = this.posA.clone().add(this.posB).add(new Vector(1.0, 1.0, 1.0)).multiply(0.5);
   }

   private void alignCoordinates() {
      this.posA = MathUtil.min(this.enteredPosA, this.enteredPosB);
      this.posB = MathUtil.max(this.enteredPosA, this.enteredPosB);
   }

   public PortalSelection clone() {
      try {
         PortalSelection clone = (PortalSelection)super.clone();
         clone.logger = this.logger;
         clone.posA = this.posA;
         clone.posB = this.posB;
         clone.portalLocation = this.portalLocation;
         clone.portalDirection = this.portalDirection;
         clone.portalSize = this.portalSize;
         clone.isValid = this.isValid;
         return clone;
      } catch (CloneNotSupportedException var2) {
         throw new RuntimeException(var2);
      }
   }

   @Nullable
   @Override
   public Location getPosA() {
      return this.posA;
   }

   @Nullable
   @Override
   public Location getPosB() {
      return this.posB;
   }
}
