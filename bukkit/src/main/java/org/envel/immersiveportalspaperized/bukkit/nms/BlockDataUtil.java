package org.envel.immersiveportalspaperized.bukkit.nms;

import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityType;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import java.lang.reflect.Method;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.HangingSign;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.util.ReflectionUtil;

/**
 * BlockDataUtil.
 */
public class BlockDataUtil {
   private static Method GET_STATE;
   private static Method GET_ID;
   private static Method STATE_BY_ID;
   private static Method FROM_DATA;
   private static final BlockData DEFAULT_BLOCK_DATA = Bukkit.createBlockData(Material.AIR);

   // getCombinedId / getByCombinedId are unrelated to ProtocolLib or PacketEvents - always were
   // raw NMS reflection for turning a BlockData into/from a plain int for the cross-server
   // external-block-watching feature. Left untouched by this migration.
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

   /**
    * NOTE on this migration: for {@link Sign} (and {@link HangingSign}) tile states, this now
    * builds a real {@code front_text} / {@code back_text} NBT payload matching the post-1.20
    * two-sided sign block entity format, so sign and hanging sign text renders correctly through
    * portals. This gap was confirmed via an in-game test: block rendering itself (via
    * {@code ModernMultiBlockChangeManager}) worked fine, but sign text stayed blank - that's what
    * prompted implementing this instead of leaving it a pure architectural guess.
    * <p>
    * All other tile entity types (chests, etc.) still intentionally return {@code null} - every
    * caller already null-checks this, so that's a safe, non-breaking choice. This is a deliberate
    * decision, not a "not implemented yet": since the plugin is server-side-only (no client mod),
    * a player can never actually reach through a portal to open a chest rendered on the other
    * side - the server resolves interactions against the real block at the player's real
    * position, not the rendered illusion. A chest's contents would also change far more often
    * than sign text, making poll-interval staleness visibly noticeable, and ItemStack/container
    * NBT (enchantments, custom item components, etc.) is substantially more complex to build
    * correctly than four text lines. Given that the one thing container content would be useful
    * for (interacting with it) doesn't work regardless, the cosmetic-only benefit doesn't justify
    * the added complexity and staleness risk. Revisit only if a concrete need comes up.
    */
   @Nullable
   public static WrapperPlayServerBlockEntityData getUpdatePacket(@NotNull BlockState tileState) {
      if (!(tileState instanceof TileState)) {
         return null;
      } else if (tileState instanceof Sign sign) {
         return getSignUpdatePacket(sign);
      } else {
         return null;
      }
   }

   @NotNull
   private static WrapperPlayServerBlockEntityData getSignUpdatePacket(@NotNull Sign sign) {
      NBTCompound nbt = new NBTCompound();
      nbt.setTag("front_text", buildSignSideCompound(sign.getSide(Side.FRONT)));
      nbt.setTag("back_text", buildSignSideCompound(sign.getSide(Side.BACK)));
      nbt.setTag("is_waxed", new NBTByte(sign.isWaxed()));
      BlockEntityType type = sign instanceof HangingSign ? BlockEntityTypes.HANGING_SIGN : BlockEntityTypes.SIGN;
      // Position is a placeholder (0, 0, 0) here - setTileEntityPosition() below overwrites it
      // with the real origin/destination position once the caller (BukkitBlockMap) knows which
      // one applies, same pattern the ProtocolLib version used.
      return new WrapperPlayServerBlockEntityData(new Vector3i(0, 0, 0), type, nbt);
   }

   @NotNull
   private static NBTCompound buildSignSideCompound(@NotNull SignSide side) {
      NBTCompound sideCompound = new NBTCompound();
      sideCompound.setTag("has_glowing_text", new NBTByte(side.isGlowingText()));
      DyeColor color = side.getColor();
      sideCompound.setTag("color", new NBTString((color != null ? color : DyeColor.BLACK).name().toLowerCase(Locale.ROOT)));
      NBTList<NBTString> messages = NBTList.createStringList();

      for (Component line : side.lines()) {
         // Each sign text line is stored client-side as a JSON text component string, not raw
         // text - GsonComponentSerializer preserves colors/formatting the same way the vanilla
         // sign-editing client itself would have written them into this NBT tag.
         messages.addTag(new NBTString(GsonComponentSerializer.gson().serialize(line)));
      }

      sideCompound.setTag("messages", messages);
      return sideCompound;
   }

   public static void setTileEntityPosition(@NotNull WrapperPlayServerBlockEntityData packet, @NotNull IntVector position) {
      packet.setPosition(new Vector3i(position.getX(), position.getY(), position.getZ()));
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


