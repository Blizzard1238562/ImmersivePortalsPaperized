package org.envel.immersiveportalspaperized.bukkit.nms;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RotationUtil {
   private static final Map<BlockFace, Vector> directionToVector = new HashMap<>();
   private static final Map<Vector, BlockFace> vectorToDirection = new HashMap<>();
   private static final Map<Integer, BlockFace> idToDirection = new HashMap<>();
   private static final Map<BlockFace, Integer> directionToId = new HashMap<>();

   @NotNull
   public static Vector getVector(@NotNull BlockFace direction) {
      return directionToVector.get(direction);
   }

   @Nullable
   public static BlockFace getDirection(@NotNull Vector vector) {
      vector = MathUtil.round(vector.clone().normalize());
      return vectorToDirection.get(vector);
   }

   public static int getId(@NotNull BlockFace direction) {
      return directionToId.get(direction);
   }

   @Nullable
   public static BlockFace getDirection(int id) {
      return idToDirection.get(id);
   }

   @Nullable
   public static BlockFace rotateBy(BlockFace direction, Matrix matrix) {
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
      directionToVector.put(BlockFace.DOWN, new Vector(0.0, -1.0, 0.0));
      directionToVector.put(BlockFace.UP, new Vector(0.0, 1.0, 0.0));
      directionToVector.put(BlockFace.EAST, new Vector(1.0, 0.0, 0.0));
      directionToVector.put(BlockFace.WEST, new Vector(-1.0, 0.0, 0.0));
      directionToVector.put(BlockFace.NORTH, new Vector(0.0, 0.0, -1.0));
      directionToVector.put(BlockFace.SOUTH, new Vector(0.0, 0.0, 1.0));
      directionToVector.forEach((direction, vector) -> vectorToDirection.put(vector, direction));
      idToDirection.put(0, BlockFace.DOWN);
      idToDirection.put(1, BlockFace.UP);
      idToDirection.put(2, BlockFace.NORTH);
      idToDirection.put(3, BlockFace.SOUTH);
      idToDirection.put(4, BlockFace.WEST);
      idToDirection.put(5, BlockFace.EAST);
      idToDirection.forEach((id, direction) -> directionToId.put(direction, id));
   }
}
