package org.envel.immersiveportalspaperized.bukkit.block.rotation;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Axis;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.jetbrains.annotations.Nullable;

public class AxisUtil {
   private static final Map<Vector, Axis> vectorToAxis = new HashMap<>();
   private static final Map<Axis, Vector> axisToVector = new HashMap<>();

   @Nullable
   public static Axis rotateAxis(Axis axis, Matrix matrix) {
      Vector oldRotation = axisToVector.get(axis);
      Vector newRotation = MathUtil.round(matrix.transform(oldRotation));
      return vectorToAxis.get(newRotation);
   }

   static {
      vectorToAxis.put(new Vector(1.0, 0.0, 0.0), Axis.X);
      vectorToAxis.put(new Vector(-1.0, 0.0, 0.0), Axis.X);
      vectorToAxis.put(new Vector(0.0, 1.0, 0.0), Axis.Y);
      vectorToAxis.put(new Vector(0.0, -1.0, 0.0), Axis.Y);
      vectorToAxis.put(new Vector(0.0, 0.0, 1.0), Axis.Z);
      vectorToAxis.put(new Vector(0.0, 0.0, -1.0), Axis.Z);
      axisToVector.put(Axis.X, new Vector(1.0, 0.0, 0.0));
      axisToVector.put(Axis.Y, new Vector(0.0, 1.0, 0.0));
      axisToVector.put(Axis.Z, new Vector(0.0, 0.0, 1.0));
   }
}
