package org.envel.immersiveportalspaperized.bukkit.math;

import java.io.Serializable;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.api.PortalDirection;

/**
 * Matrix.
 */
public class Matrix implements Serializable {
   private static final long serialVersionUID = 1L;
   public double[][] m;

   public Matrix(double[][] matrix) {
      this.m = matrix;
   }

   private Matrix() {
      this.m = new double[][]{{0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0}};
   }

   public static Matrix makeTranslation(Vector offset) {
      return new Matrix(new double[][]{{1.0, 0.0, 0.0, offset.getX()}, {0.0, 1.0, 0.0, offset.getY()}, {0.0, 0.0, 1.0, offset.getZ()}, {0.0, 0.0, 0.0, 1.0}});
   }

   public static Matrix makeIdentity() {
      return new Matrix(new double[][]{{1.0, 0.0, 0.0, 0.0}, {0.0, 1.0, 0.0, 0.0}, {0.0, 0.0, 1.0, 0.0}, {0.0, 0.0, 0.0, 1.0}});
   }

   public static Matrix makeRotation(PortalDirection from, PortalDirection to) {
      Vector fromVec = from.toVector();
      Vector toVec = to.toVector();
      return fromVec.equals(toVec.clone().multiply(-1.0)) ? makeRotation(from.getInversionRotationAxis(), Math.PI) : makeRotation(fromVec, toVec);
   }

   public static Matrix makeRotation(Vector from, Vector to) {
      double angle = from.angle(to);
      Vector axis = from.getCrossProduct(to);
      return makeRotation(axis, angle);
   }

   public Matrix multiply(Matrix other) {
      Matrix result = new Matrix();

      for (int x = 0; x < 4; x++) {
         for (int y = 0; y < 4; y++) {
            result.m[y][x] = this.m[y][0] * other.m[0][x] + this.m[y][1] * other.m[1][x] + this.m[y][2] * other.m[2][x] + this.m[y][3] * other.m[3][x];
         }
      }

      return result;
   }

   public static Matrix makeRotation(Vector axis, double angle) {
      double uX = axis.getX();
      double uY = axis.getY();
      double uZ = axis.getZ();
      double cosAngle = Math.cos(angle);
      double sinAngle = Math.sin(angle);
      return new Matrix(
         new double[][]{
            {cosAngle + uX * uX * (1.0 - cosAngle), uX * uY * (1.0 - cosAngle) - uZ * sinAngle, uX * uZ * (1.0 - cosAngle) + uY * sinAngle, 0.0},
            {uY * uX * (1.0 - cosAngle) + uZ * sinAngle, cosAngle + uY * uY * (1.0 - cosAngle), uY * uZ * (1.0 - cosAngle) - uX * sinAngle, 0.0},
            {uZ * uX * (1.0 - cosAngle) - uY * sinAngle, uZ * uY * (1.0 - cosAngle) + uX * sinAngle, cosAngle + uZ * uZ * (1.0 - cosAngle), 0.0},
            {0.0, 0.0, 0.0, 1.0}
         }
      );
   }

   public Vector transform(Vector in) {
      double[] result = new double[4];

      for (int i = 0; i < 4; i++) {
         result[i] = in.getX() * this.m[i][0] + in.getY() * this.m[i][1] + in.getZ() * this.m[i][2] + this.m[i][3];
      }

      return new Vector(result[0] / result[3], result[1] / result[3], result[2] / result[3]);
   }

   public IntVector transform(int x, int y, int z) {
      float[] result = new float[4];

      for (int i = 0; i < 4; i++) {
         result[i] = (float)(x * this.m[i][0] + y * this.m[i][1] + z * this.m[i][2] + this.m[i][3]);
      }

      return new IntVector(Math.round(result[0] / result[3]), Math.round(result[1] / result[3]), Math.round(result[2] / result[3]));
   }

   public IntVector transform(IntVector in) {
      return this.transform(in.getX(), in.getY(), in.getZ());
   }
}


