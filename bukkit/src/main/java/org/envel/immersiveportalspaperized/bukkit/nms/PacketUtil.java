package org.envel.immersiveportalspaperized.bukkit.nms;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Thin wrapper around PacketEvents' {@code PlayerManager#sendPacket}.
 * <p>
 * Before the ProtocolLib -&gt; PacketEvents migration, this class also held several
 * ProtocolLib-specific {@code StructureModifier} field-writing helpers
 * ({@code writeDoublePosition}, {@code readDoublePosition}, {@code writeRelativeOffset},
 * {@code writeVelocity}, {@code writeTeleportRotation}, {@code writeLookRotation}). Those
 * intentionally have no equivalent here: PacketEvents' typed {@code WrapperPlayServerXxx}
 * classes take every field through named constructor parameters, so callers now construct a
 * fully-formed packet directly instead of building an empty {@code PacketContainer} and
 * writing fields into it afterwards. See {@code entity/faking/EntityPacketManipulator.java}
 * for the new construction pattern once it's migrated.
 */
public class PacketUtil {
   public static void sendPacket(@NotNull Player player, @NotNull PacketWrapper<?> packet) {
      PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
   }

   public static void sendPacket(@NotNull Collection<Player> players, @NotNull PacketWrapper<?> packet) {
      for (Player player : players) {
         PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
      }
   }
}
