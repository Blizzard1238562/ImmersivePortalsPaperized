package org.envel.immersiveportalspaperized.bukkit.block.rotation;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Rail.Shape;
import org.bukkit.block.data.type.Fence;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.jetbrains.annotations.NotNull;

public class ModernBlockRotator implements IBlockRotator {
   @NotNull
   @Override
   public BlockData rotateByMatrix(@NotNull Matrix matrix, @NotNull BlockData data) {
      if (data instanceof Rotatable) {
         Rotatable rotatable = (Rotatable)data.clone();
         BlockFace currentFace = rotatable.getRotation();
         BlockFace rotatedFace = BlockFaceUtil.rotateFace(currentFace, matrix);
         if (rotatedFace != null) {
            try {
               rotatable.setRotation(rotatedFace);
            } catch (IllegalArgumentException var8) {
               rotatable.setRotation(currentFace);
            }

            return rotatable;
         }
      }

      if (data instanceof Directional) {
         Directional directional = (Directional)data.clone();
         BlockFace currentFace = directional.getFacing();
         BlockFace rotatedFace = BlockFaceUtil.rotateFace(currentFace, matrix);
         if (rotatedFace != null && directional.getFaces().contains(rotatedFace)) {
            directional.setFacing(rotatedFace);
            return directional;
         }
      }

      if (data instanceof Orientable) {
         Orientable orientable = (Orientable)data.clone();
         Axis currentAxis = orientable.getAxis();
         Axis rotatedAxis = AxisUtil.rotateAxis(currentAxis, matrix);
         if (rotatedAxis != null && orientable.getAxes().contains(rotatedAxis)) {
            orientable.setAxis(rotatedAxis);
            return orientable;
         }
      }

      if (data instanceof Rail) {
         Rail rail = (Rail)data.clone();
         Shape currentShape = rail.getShape();
         Shape rotatedShape = RailUtil.rotateRailShape(currentShape, matrix);
         if (rotatedShape != null && rail.getShapes().contains(rotatedShape)) {
            rail.setShape(rotatedShape);
            return rail;
         }
      }

      if (!(data instanceof Fence)) {
         return data;
      } else {
         Fence fence = (Fence)data.clone();
         List<BlockFace> newFaces = new ArrayList<>();

         for (BlockFace face : fence.getFaces()) {
            BlockFace rotated = BlockFaceUtil.rotateFace(face, matrix);
            if (rotated != null) {
               newFaces.add(rotated);
            }

            fence.setFace(face, false);
         }

         for (BlockFace face : newFaces) {
            fence.setFace(face, true);
         }

         return fence;
      }
   }
}
