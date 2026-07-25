package org.envel.immersiveportalspaperized.bukkit.nms;

import com.comphenix.protocol.wrappers.EnumWrappers.Direction;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RotationUtil {
   private static final Map<Direction, Vector> directionToVector = new HashMap<>();
   private static final Map<Vector, Direction> vectorToDirection = new HashMap<>();
   private static final Map<Integer, Direction> idToDirection = new HashMap<>();
   private static final Map<Direction, Integer> directionToId = new HashMap<>();

   @NotNull
   public static Vector getVector(@NotNull Direction direction) {
      return directionToVector.get(direction);
   }

   @Nullable
   public static Direction getDirection(@NotNull Vector vector) {
      vector = MathUtil.round(vector.clone().normalize());
      return vectorToDirection.get(vector);
   }

   public static int getId(@NotNull Direction direction) {
      return directionToId.get(direction);
   }

   @Nullable
   public static Direction getDirection(int id) {
      return idToDirection.get(id);
   }

   @Nullable
   public static Direction rotateBy(Direction direction, Matrix matrix) {
      Vector finalDir = matrix.transform(getVector(direction));
      return getDirection(finalDir);
   }

   public static int getPacketRotationInt(float angle) {
      float limited = angle * 256.0F / 360.0F;
      int clamped = (int)limited;
      return limited < clamped ? clamped - 1 : clamped;
   }

   public static byte getPacketRotationByte(float angle) {
      return (byte)(angle * 256.0F / 360.0F);
   }

   static {
      directionToVector.put(Direction.DOWN, new Vector(0.0, -1.0, 0.0));
      directionToVector.put(Direction.UP, new Vector(0.0, 1.0, 0.0));
      directionToVector.put(Direction.EAST, new Vector(1.0, 0.0, 0.0));
      directionToVector.put(Direction.WEST, new Vector(-1.0, 0.0, 0.0));
      directionToVector.put(Direction.NORTH, new Vector(0.0, 0.0, -1.0));
      directionToVector.put(Direction.SOUTH, new Vector(0.0, 0.0, 1.0));
      directionToVector.forEach((direction, vector) -> vectorToDirection.put(vector, direction));
      idToDirection.put(0, Direction.DOWN);
      idToDirection.put(1, Direction.UP);
      idToDirection.put(2, Direction.NORTH);
      idToDirection.put(3, Direction.SOUTH);
      idToDirection.put(4, Direction.WEST);
      idToDirection.put(5, Direction.EAST);
      idToDirection.forEach((id, direction) -> directionToId.put(direction, id));
   }
}
