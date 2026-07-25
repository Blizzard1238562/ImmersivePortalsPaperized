package org.envel.immersiveportalspaperized.bukkit.block.rotation;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.data.Rail.Shape;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.jetbrains.annotations.Nullable;

public class RailUtil {
   private static final Map<Shape, RailUtil.RailShapeSet> shapeToRotationSet = new HashMap<>();

   @Nullable
   public static Shape rotateRailShape(Shape shape, Matrix matrix) {
      RailUtil.RailShapeSet set = shapeToRotationSet.get(shape);
      Vector direction = set.shapeToDirection.get(shape);
      Vector rotatedDir = MathUtil.round(matrix.transform(direction));
      Shape rotated = set.directionToShape.get(rotatedDir);
      return rotated == null ? shape : rotated;
   }

   static {
      Map<Vector, Shape> straightSet = new HashMap<>();
      straightSet.put(new Vector(1.0, 0.0, 0.0), Shape.EAST_WEST);
      straightSet.put(new Vector(-1.0, 0.0, 0.0), Shape.EAST_WEST);
      straightSet.put(new Vector(0.0, 0.0, 1.0), Shape.NORTH_SOUTH);
      straightSet.put(new Vector(0.0, 0.0, -1.0), Shape.NORTH_SOUTH);
      new RailUtil.RailShapeSet(straightSet);
      Map<Vector, Shape> curvedSet = new HashMap<>();
      curvedSet.put(new Vector(0.0, 0.0, -1.0), Shape.NORTH_EAST);
      curvedSet.put(new Vector(-1.0, 0.0, 0.0), Shape.NORTH_WEST);
      curvedSet.put(new Vector(0.0, 0.0, 1.0), Shape.SOUTH_WEST);
      curvedSet.put(new Vector(1.0, 0.0, 0.0), Shape.SOUTH_EAST);
      new RailUtil.RailShapeSet(curvedSet);
      Map<Vector, Shape> ascendingSet = new HashMap<>();
      ascendingSet.put(new Vector(1.0, 0.0, 0.0), Shape.ASCENDING_EAST);
      ascendingSet.put(new Vector(-1.0, 0.0, 0.0), Shape.ASCENDING_WEST);
      ascendingSet.put(new Vector(0.0, 0.0, 1.0), Shape.ASCENDING_SOUTH);
      ascendingSet.put(new Vector(0.0, 0.0, -1.0), Shape.ASCENDING_NORTH);
      new RailUtil.RailShapeSet(ascendingSet);
   }

   private static class RailShapeSet {
      Map<Vector, Shape> directionToShape;
      Map<Shape, Vector> shapeToDirection = new HashMap<>();

      public RailShapeSet(Map<Vector, Shape> directionToShape) {
         this.directionToShape = directionToShape;
         directionToShape.forEach((direction, shape) -> {
            this.shapeToDirection.put(shape, direction);
            RailUtil.shapeToRotationSet.put(shape, this);
         });
      }
   }
}
