package org.envel.immersiveportalspaperized.bukkit.block.rotation;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockFaceUtil {
   private static final Map<Vector, BlockFace> vectorToBlockFace = new HashMap<>();

   @NotNull
   private static Vector getDirection(BlockFace face) {
      Vector direction = new Vector(face.getModX(), face.getModY(), face.getModZ());
      direction.normalize();
      return direction;
   }

   @Nullable
   public static BlockFace rotateFace(BlockFace face, Matrix matrix) {
      Vector oldRotation = getDirection(face);
      Vector newRotation = MathUtil.round(matrix.transform(oldRotation));
      return vectorToBlockFace.get(newRotation);
   }

   static {
      for (BlockFace variant : BlockFace.class.getEnumConstants()) {
         vectorToBlockFace.put(getDirection(variant), variant);
      }
   }
}
