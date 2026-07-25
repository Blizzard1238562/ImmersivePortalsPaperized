package org.envel.immersiveportalspaperized.bukkit.nms;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class PacketUtil {
   public static void writeRelativeOffset(@NotNull PacketContainer packet, @NotNull Vector offset) {
      StructureModifier<Short> shorts = packet.getShorts();
      shorts.write(0, (short)(offset.getX() * 4096.0));
      shorts.write(1, (short)(offset.getY() * 4096.0));
      shorts.write(2, (short)(offset.getZ() * 4096.0));
   }

   public static void writeVelocity(@NotNull PacketContainer packet, @NotNull Vector velocity) {
      StructureModifier<Integer> integers = packet.getIntegers();
      integers.write(1, (int)(velocity.getX() * 8000.0));
      integers.write(2, (int)(velocity.getY() * 8000.0));
      integers.write(3, (int)(velocity.getZ() * 8000.0));
   }

   public static void writeDoublePosition(@NotNull PacketContainer packet, @NotNull Vector position) {
      StructureModifier<Double> doubles = packet.getDoubles();
      if (doubles.size() >= 3) {
         doubles.write(0, position.getX());
         doubles.write(1, position.getY());
         doubles.write(2, position.getZ());
      } else {
         try {
            Object nmsPacket = packet.getHandle();

            try {
               Field xField = nmsPacket.getClass().getDeclaredField("x");
               Field yField = nmsPacket.getClass().getDeclaredField("y");
               Field zField = nmsPacket.getClass().getDeclaredField("z");
               xField.setAccessible(true);
               yField.setAccessible(true);
               zField.setAccessible(true);
               xField.setDouble(nmsPacket, position.getX());
               yField.setDouble(nmsPacket, position.getY());
               zField.setDouble(nmsPacket, position.getZ());
               return;
            } catch (NoSuchFieldException var11) {
            }

            for (Field field : nmsPacket.getClass().getDeclaredFields()) {
               Class<?> fieldType = field.getType();
               if (!fieldType.isPrimitive() && !fieldType.isArray() && !fieldType.isEnum()) {
                  try {
                     Constructor<?> constr = fieldType.getConstructor(double.class, double.class, double.class);
                     constr.setAccessible(true);
                     Object vec = constr.newInstance(position.getX(), position.getY(), position.getZ());
                     field.setAccessible(true);
                     field.set(nmsPacket, vec);
                     return;
                  } catch (NoSuchMethodException var12) {
                  }
               }
            }
         } catch (Throwable var13) {
         }
      }
   }

   public static Vector readDoublePosition(@NotNull PacketContainer packet) {
      StructureModifier<Double> doubles = packet.getDoubles();
      if (doubles.size() >= 3) {
         return new Vector((Double)doubles.read(0), (Double)doubles.read(1), (Double)doubles.read(2));
      } else {
         try {
            Object nmsPacket = packet.getHandle();

            try {
               Field xField = nmsPacket.getClass().getDeclaredField("x");
               Field yField = nmsPacket.getClass().getDeclaredField("y");
               Field zField = nmsPacket.getClass().getDeclaredField("z");
               xField.setAccessible(true);
               yField.setAccessible(true);
               zField.setAccessible(true);
               return new Vector(xField.getDouble(nmsPacket), yField.getDouble(nmsPacket), zField.getDouble(nmsPacket));
            } catch (NoSuchFieldException var16) {
               for (Field field : nmsPacket.getClass().getDeclaredFields()) {
                  Class<?> fieldType = field.getType();
                  if (!fieldType.isPrimitive() && !fieldType.isArray() && !fieldType.isEnum()) {
                     try {
                        fieldType.getConstructor(double.class, double.class, double.class);
                        field.setAccessible(true);
                        Object vec = field.get(nmsPacket);
                        if (vec != null) {
                           double x = (Double)vec.getClass().getMethod("x").invoke(vec);
                           double y = (Double)vec.getClass().getMethod("y").invoke(vec);
                           double z = (Double)vec.getClass().getMethod("z").invoke(vec);
                           return new Vector(x, y, z);
                        }
                     } catch (NoSuchMethodException var15) {
                     }
                  }
               }
            }
         } catch (Throwable var17) {
         }

         return new Vector(0, 0, 0);
      }
   }

   public static void writeTeleportRotation(@NotNull PacketContainer packet, float yaw, float pitch) {
      StructureModifier<Byte> bytes = packet.getBytes();
      bytes.write(0, RotationUtil.getPacketRotationByte(yaw));
      bytes.write(1, RotationUtil.getPacketRotationByte(pitch));
   }

   public static void writeLookRotation(@NotNull PacketContainer packet, float yaw, float pitch) {
      StructureModifier<Byte> bytes = packet.getBytes();
      bytes.write(0, (byte)RotationUtil.getPacketRotationInt(yaw));
      bytes.write(1, (byte)RotationUtil.getPacketRotationInt(pitch));
   }

   public static void sendPacket(@NotNull Player player, @NotNull PacketContainer packet) {
      try {
         ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
      } catch (Exception var3) {
         throw new RuntimeException("Failed to send packet to " + player.getName(), var3);
      }
   }

   public static void sendPacket(@NotNull Collection<Player> players, @NotNull PacketContainer packet) {
      ProtocolManager pm = ProtocolLibrary.getProtocolManager();

      try {
         for (Player player : players) {
            pm.sendServerPacket(player, packet);
         }
      } catch (Exception var5) {
         throw new RuntimeException("Failed to send packet to players", var5);
      }
   }
}
