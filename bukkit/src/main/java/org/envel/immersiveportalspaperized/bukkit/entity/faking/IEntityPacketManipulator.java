package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.nms.AnimationType;

public interface IEntityPacketManipulator {
   void showEntity(EntityInfo tracker, Collection<Player> players);

   default void showEntity(EntityInfo tracker, Player player) {
      this.showEntity(tracker, Collections.singleton(player));
   }

   void hideEntity(EntityInfo tracker, Collection<Player> players);

   default void hideEntity(EntityInfo tracker, Player player) {
      this.hideEntity(tracker, Collections.singleton(player));
   }

   void sendEntityMove(EntityInfo tracker, Vector offset, Collection<Player> players);

   void sendEntityMoveLook(EntityInfo tracker, Vector offset, Collection<Player> players);

   void sendEntityLook(EntityInfo tracker, Collection<Player> players);

   void sendEntityTeleport(EntityInfo tracker, Collection<Player> players);

   void sendEntityHeadRotation(EntityInfo tracker, Collection<Player> players);

   void sendMount(EntityInfo tracker, Collection<EntityInfo> riding, Collection<Player> players);

   void sendEntityEquipment(EntityInfo tracker, Map<ItemSlot, ItemStack> changes, Collection<Player> players);

   void sendMetadata(EntityInfo tracker, Collection<Player> players);

   void sendEntityVelocity(EntityInfo tracker, Vector newVelocity, Collection<Player> players);

   void sendEntityAnimation(EntityInfo tracker, Collection<Player> players, AnimationType animationType);

   void sendEntityPickupItem(EntityInfo tracker, EntityInfo pickedUp, Collection<Player> players);

   void sendAddPlayerProfile(EntityInfo tracker, Collection<Player> players);

   void sendRemovePlayerProfile(EntityInfo tracker, Collection<Player> players);
}
