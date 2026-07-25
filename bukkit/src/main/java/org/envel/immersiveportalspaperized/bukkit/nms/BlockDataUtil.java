package org.envel.immersiveportalspaperized.bukkit.nms;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.util.ReflectionUtil;

public class BlockDataUtil {
   private static Method GET_STATE;
   private static Method GET_ID;
   private static Method STATE_BY_ID;
   private static Method FROM_DATA;
   private static final BlockData DEFAULT_BLOCK_DATA = Bukkit.createBlockData(Material.AIR);

   public static int getCombinedId(@NotNull BlockData blockData) {
      if (GET_STATE != null && GET_ID != null) {
         try {
            Object nmsState = ReflectionUtil.invokeMethod(blockData, GET_STATE);
            return (Integer)ReflectionUtil.invokeMethod(null, GET_ID, nmsState);
         } catch (Exception var2) {
         }
      }

      return blockData.getMaterial().ordinal();
   }

   public static BlockData getByCombinedId(int combinedId) {
      if (STATE_BY_ID != null && FROM_DATA != null) {
         try {
            Object nmsState = ReflectionUtil.invokeMethod(null, STATE_BY_ID, combinedId);
            if (nmsState != null) {
               return (BlockData)ReflectionUtil.invokeMethod(null, FROM_DATA, nmsState);
            }
         } catch (Exception var3) {
         }
      }

      Material[] materials = Material.values();
      if (combinedId >= 0 && combinedId < materials.length) {
         Material mat = materials[combinedId];
         if (mat.isBlock()) {
            return Bukkit.createBlockData(mat);
         }
      }

      return DEFAULT_BLOCK_DATA;
   }

   @Nullable
   public static PacketContainer getUpdatePacket(@NotNull BlockState tileState) {
      if (!(tileState instanceof TileState)) {
         return null;
      } else {
         ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
         return protocolManager.createPacket(Server.TILE_ENTITY_DATA);
      }
   }

   public static void setTileEntityPosition(@NotNull PacketContainer packet, @NotNull IntVector position) {
      BlockPosition blockPosition = new BlockPosition(position.getX(), position.getY(), position.getZ());
      packet.getBlockPositionModifier().write(0, blockPosition);
   }

   static {
      try {
         Class<?> craftBlockDataClass = CraftBukkitClassUtil.findCraftBukkitClass("block.data.CraftBlockData");
         GET_STATE = ReflectionUtil.findMethod(craftBlockDataClass, "getState");
         Class<?> nmsBlockClass = ReflectionUtil.findClass("net.minecraft.world.level.block.Block");
         Class<?> nmsBlockStateClass = null;

         try {
            nmsBlockStateClass = ReflectionUtil.findClass("net.minecraft.world.level.block.state.BlockState");
         } catch (Exception var14) {
            try {
               nmsBlockStateClass = ReflectionUtil.findClass("net.minecraft.world.level.block.state.IBlockData");
            } catch (Exception var13) {
            }
         }

         if (nmsBlockStateClass != null) {
            try {
               GET_ID = ReflectionUtil.findMethod(nmsBlockClass, "getId", nmsBlockStateClass);
            } catch (Exception var12) {
               try {
                  GET_ID = ReflectionUtil.findMethod(nmsBlockClass, "getCombinedId", nmsBlockStateClass);
               } catch (Exception var11) {
                  try {
                     GET_ID = ReflectionUtil.findMethod(nmsBlockClass, "i", nmsBlockStateClass);
                  } catch (Exception var10) {
                  }
               }
            }

            try {
               STATE_BY_ID = ReflectionUtil.findMethod(nmsBlockClass, "stateById", int.class);
            } catch (Exception var9) {
               try {
                  STATE_BY_ID = ReflectionUtil.findMethod(nmsBlockClass, "getByCombinedId", int.class);
               } catch (Exception var8) {
                  try {
                     STATE_BY_ID = ReflectionUtil.findMethod(nmsBlockClass, "a", int.class);
                  } catch (Exception var7) {
                  }
               }
            }

            try {
               FROM_DATA = ReflectionUtil.findMethod(craftBlockDataClass, "fromData", nmsBlockStateClass);
            } catch (Exception var6) {
            }
         }
      } catch (Exception var15) {
         var15.printStackTrace();
      }
   }
}
