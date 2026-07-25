package org.envel.immersiveportalspaperized.bukkit.block.rotation;

import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.bukkit.math.Matrix;
import org.jetbrains.annotations.NotNull;

public interface IBlockRotator {
   @NotNull
   BlockData rotateByMatrix(@NotNull Matrix matrix, @NotNull BlockData data);
}
