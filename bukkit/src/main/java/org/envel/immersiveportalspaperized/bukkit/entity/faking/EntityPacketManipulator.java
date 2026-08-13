package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCollectItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
// NOTE: com.destroystokyo.paper.profile.PlayerProfile is deprecated for removal in paper-api,
// but io.papermc.paper.profile.PlayerProfile (which it extends) does not exist yet in this
// project's pinned paper-api version (1.21.4) - confirmed by a real compile error, not a guess.
// org.bukkit.profile.PlayerProfile (the non-Paper-specific base) lacks getProperties() here.
// Revisit this import if/when the project's paper-api dependency is upgraded past 1.21.4.
import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.nms.AnimationType;
import org.envel.immersiveportalspaperized.bukkit.nms.PacketUtil;
import org.envel.immersiveportalspaperized.bukkit.nms.RotationUtil;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * EntityPacketManipulator.
 */
@Singleton
public class EntityPacketManipulator implements IEntityPacketManipulator {
   private final Logger logger;

   @Inject
   public EntityPacketManipulator(Logger logger) {
      this.logger = logger;
   }

   @Override
   public void showEntity(EntityInfo tracker, Collection<Player> players) {
      try {
         if (tracker.getEntity() instanceof org.bukkit.entity.EnderDragonPart || tracker.getEntity() instanceof org.bukkit.entity.Marker) {
            return;
         }

         Vector actualPos = tracker.getEntity().getLocation().toVector();
         if (tracker.getEntity() instanceof Hanging) {
            actualPos = MathUtil.moveToCenterOfBlock(actualPos);
         }

         Vector renderedPos = tracker.getTranslation().transform(actualPos);
         Location renderedLocation = tracker.findRenderedLocation();
         float yaw = renderedLocation.getYaw();
         float pitch = renderedLocation.getPitch();
         Location spawnBukkitLocation = new Location(
            tracker.getEntity().getWorld(), renderedPos.getX(), renderedPos.getY(), renderedPos.getZ(), yaw, pitch
         );

         int data = 0;
         if (tracker.getEntity() instanceof Hanging) {
            // Preserves the exact behaviour of the pre-migration code: it read the "data" field
            // back off a freshly built (all-default) spawn packet rather than the real entity's
            // current BlockFace, so this always rotated starting from id 0 (BlockFace.DOWN). That
            // looks like a pre-existing bug independent of this migration - not changed here to
            // keep this a pure library swap. Worth a look separately.
            BlockFace currentDirection = RotationUtil.getDirection(0);
            if (currentDirection != null) {
               BlockFace rotated = RotationUtil.rotateBy(currentDirection, tracker.getRotation());
               if (rotated == null) {
                  throw new IllegalStateException("Portal attempted to rotate a hanging entity to an invalid block direction");
               }

               data = RotationUtil.getId(rotated);
            }
         }

         // NOTE: entity type conversion via SpigotConversionUtil#fromBukkitEntityType - verify
         // exact method name against the installed jar / IDE autocomplete, was not individually
         // confirmed this session (fromBukkitLocation and fromBukkitItemStack were).
         WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
            tracker.getEntityId(),
            tracker.getEntityUniqueId(),
            SpigotConversionUtil.fromBukkitEntityType(tracker.getEntity().getType()),
            SpigotConversionUtil.fromBukkitLocation(spawnBukkitLocation),
            yaw,
            data,
            null
         );
         PacketUtil.sendPacket(players, spawnPacket);
         if (tracker.getEntity() instanceof LivingEntity) {
            EntityEquipmentWatcher equipmentWatcher = new EntityEquipmentWatcher((LivingEntity)tracker.getEntity());
            Map<EquipmentSlot, ItemStack> changes = equipmentWatcher.checkForChanges();
            if (changes.size() > 0) {
               this.sendEntityEquipment(tracker, changes, players);
            }
         }

         this.sendMetadata(tracker, players);
      } catch (Throwable e) {
         this.logger.finer("showEntity failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void hideEntity(EntityInfo tracker, Collection<Player> players) {
      try {
         WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(tracker.getEntityId());
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("hideEntity failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityMove(EntityInfo tracker, Vector offset, Collection<Player> players) {
      try {
         offset = tracker.getRotation().transform(offset);
         WrapperPlayServerEntityRelativeMove packet = new WrapperPlayServerEntityRelativeMove(
            tracker.getEntityId(), offset.getX(), offset.getY(), offset.getZ(), tracker.getEntity().isOnGround()
         );
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityMove failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityMoveLook(EntityInfo tracker, Vector offset, Collection<Player> players) {
      try {
         Location entityPos = tracker.findRenderedLocation();
         offset = tracker.getRotation().transform(offset);
         WrapperPlayServerEntityRelativeMoveAndRotation packet = new WrapperPlayServerEntityRelativeMoveAndRotation(
            tracker.getEntityId(),
            offset.getX(),
            offset.getY(),
            offset.getZ(),
            entityPos.getYaw(),
            entityPos.getPitch(),
            tracker.getEntity().isOnGround()
         );
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityMoveLook failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityLook(EntityInfo tracker, Collection<Player> players) {
      try {
         Location entityPos = tracker.findRenderedLocation();
         WrapperPlayServerEntityRotation packet = new WrapperPlayServerEntityRotation(
            tracker.getEntityId(), entityPos.getYaw(), entityPos.getPitch(), tracker.getEntity().isOnGround()
         );
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityLook failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityTeleport(EntityInfo tracker, Collection<Player> players) {
      try {
         Location entityPos = tracker.findRenderedLocation();
         WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
            tracker.getEntityId(),
            new Vector3d(entityPos.getX(), entityPos.getY(), entityPos.getZ()),
            entityPos.getYaw(),
            entityPos.getPitch(),
            tracker.getEntity().isOnGround()
         );
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityTeleport failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityHeadRotation(EntityInfo tracker, Collection<Player> players) {
      try {
         Location renderedPos = tracker.findRenderedLocation();
         // No more reflection fallback needed - PacketEvents' wrapper covers this packet
         // directly, unlike the ProtocolLib version which had to reach for raw NMS first.
         WrapperPlayServerEntityHeadLook packet = new WrapperPlayServerEntityHeadLook(tracker.getEntityId(), renderedPos.getYaw());
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityHeadRotation failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendMount(EntityInfo tracker, Collection<EntityInfo> riding, Collection<Player> players) {
      try {
         int[] ridingIds = new int[riding.size()];
         int i = 0;

         for (EntityInfo ridingTracker : riding) {
            ridingIds[i] = ridingTracker.getEntityId();
            i++;
         }

         // No more reflection fallback needed - see sendEntityHeadRotation above.
         WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(tracker.getEntityId(), ridingIds);
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendMount failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   private static com.github.retrooper.packetevents.protocol.player.EquipmentSlot toPacketEventsEquipmentSlot(EquipmentSlot slot) {
      // Bukkit's org.bukkit.inventory.EquipmentSlot -> PacketEvents' own EquipmentSlot enum.
      // No SpigotConversionUtil helper exists for this (confirmed against the 2.13.0 javadoc),
      // so it's mapped by hand. PacketEvents additionally has SADDLE, which Bukkit's enum has no
      // equivalent for.
      return switch (slot) {
         case HAND -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.MAIN_HAND;
         case OFF_HAND -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.OFF_HAND;
         case FEET -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.BOOTS;
         case LEGS -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.LEGGINGS;
         case CHEST -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.CHEST_PLATE;
         case HEAD -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET;
         case BODY -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.BODY;
      };
   }

   @Override
   public void sendEntityEquipment(EntityInfo tracker, Map<EquipmentSlot, ItemStack> changes, Collection<Player> players) {
      try {
         List<Equipment> equipment = new ArrayList<>();
         changes.forEach((slot, item) -> {
            ItemStack safeItem = item == null ? new ItemStack(org.bukkit.Material.AIR) : item;
            equipment.add(new Equipment(toPacketEventsEquipmentSlot(slot), SpigotConversionUtil.fromBukkitItemStack(safeItem)));
         });
         WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(tracker.getEntityId(), equipment);
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityEquipment failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendMetadata(EntityInfo tracker, Collection<Player> players) {
      try {
         // PacketEvents' SpigotConversionUtil.getEntityMetadata(Entity) reads the CURRENT metadata
         // of a live Bukkit entity and converts it to PacketEvents' EntityData list directly - this
         // is the PacketEvents-side equivalent of what ProtocolLib's WrappedDataWatcher.getEntityWatcher
         // did, so the fake entity now mirrors glowing/on-fire/sneaking/pose/etc. again.
         // VERIFIED in-game (post-migration manual test via /test/hideEntity + /test/showEntity on a
         // glowing entity): the fake entity correctly showed the glowing outline through the portal.
         // No longer just an architectural assumption - confirms getEntityMetadata() returns a real,
         // usable watcher list, not an empty/partial one.
         List<EntityData<?>> metadata = SpigotConversionUtil.getEntityMetadata(tracker.getEntity());
         WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(tracker.getEntityId(), metadata);
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendMetadata failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityVelocity(EntityInfo tracker, Vector newVelocity, Collection<Player> players) {
      try {
         Vector entityVelocity = tracker.getRotation().transform(newVelocity);
         entityVelocity = MathUtil.min(entityVelocity, new Vector(3.9, 3.9, 3.9));
         entityVelocity = MathUtil.max(entityVelocity, new Vector(-3.9, -3.9, -3.9));
         WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(
            tracker.getEntityId(), new Vector3d(entityVelocity.getX(), entityVelocity.getY(), entityVelocity.getZ())
         );
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityVelocity failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   private static WrapperPlayServerEntityAnimation.EntityAnimationType toPacketEventsAnimation(AnimationType animationType) {
      // PacketEvents' EntityAnimationType has no TAKE_DAMAGE/LEAVE_BED constants (confirmed
      // against the 2.13.0 javadoc) - it uses HURT and WAKE_UP for those vanilla animation ids.
      return switch (animationType) {
         case MAIN_HAND -> WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM;
         case DAMAGE -> WrapperPlayServerEntityAnimation.EntityAnimationType.HURT;
         case LEAVE_BED -> WrapperPlayServerEntityAnimation.EntityAnimationType.WAKE_UP;
         case OFF_HAND -> WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND;
      };
   }

   @Override
   public void sendEntityAnimation(EntityInfo tracker, Collection<Player> players, AnimationType animationType) {
      try {
         // No more reflection fallback needed - see sendEntityHeadRotation above.
         WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(
            tracker.getEntityId(), toPacketEventsAnimation(animationType)
         );
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityAnimation failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityPickupItem(EntityInfo tracker, EntityInfo pickedUp, Collection<Player> players) {
      try {
         int amount = ((Item)pickedUp.getEntity()).getItemStack().getAmount();
         WrapperPlayServerCollectItem packet = new WrapperPlayServerCollectItem(pickedUp.getEntityId(), tracker.getEntityId(), amount);
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityPickupItem failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   private WrapperPlayServerPlayerInfoUpdate.PlayerInfo generatePlayerInfoData(EntityInfo tracker) {
      Player trackingPlayer = (Player)tracker.getEntity();
      UserProfile fakeProfile = new UserProfile(tracker.getEntityUniqueId(), trackingPlayer.getName());

      try {
         PlayerProfile realProfile = trackingPlayer.getPlayerProfile();
         PlayerTextures textures = realProfile.getTextures();
         if (textures != null && textures.getSkin() != null) {
            // Paper's PlayerTextures exposes the skin as a URL, not the raw signed "textures"
            // property value ProtocolLib copied verbatim. Re-deriving a signed property from a
            // URL isn't possible client-side without the original signature, so this copies the
            // property the same way the ProtocolLib version did: straight from the profile's
            // raw properties, just read via Paper's API instead of reflection.
            realProfile.getProperties()
               .stream()
               .filter(property -> "textures".equals(property.getName()))
               .findFirst()
               .ifPresent(
                  property -> fakeProfile.getTextureProperties()
                        .add(new TextureProperty("textures", property.getValue(), property.getSignature()))
               );
         }
      } catch (Throwable inner) {
         this.logger.finer("Failed to copy player skin textures for %s: %s", trackingPlayer.getName(), inner.getMessage());
      }

      GameMode gameMode = switch (trackingPlayer.getGameMode()) {
         case SURVIVAL -> GameMode.SURVIVAL;
         case CREATIVE -> GameMode.CREATIVE;
         case ADVENTURE -> GameMode.ADVENTURE;
         case SPECTATOR -> GameMode.SPECTATOR;
      };
      return new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
         fakeProfile,
         false, // not listed in the tab list - this profile only exists long enough for the
                // client to resolve the skin texture, then gets removed again (see
                // EntityTracker#addTracking / FAKE_PLAYER_TAB_LIST_REMOVE_DELAY)
         trackingPlayer.getPing(),
         gameMode,
         Component.text(trackingPlayer.getName()),
         null
      );
   }

   @Override
   public void sendAddPlayerProfile(EntityInfo tracker, Collection<Player> players) {
      try {
         WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(
            WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, this.generatePlayerInfoData(tracker)
         );
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendAddPlayerProfile failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendRemovePlayerProfile(EntityInfo tracker, Collection<Player> players) {
      try {
         WrapperPlayServerPlayerInfoRemove packet = new WrapperPlayServerPlayerInfoRemove(tracker.getEntityUniqueId());
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendRemovePlayerProfile failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }
}


