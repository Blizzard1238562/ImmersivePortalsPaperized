package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.EnumWrappers.Direction;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.math.MathUtil;
import org.envel.immersiveportalspaperized.bukkit.nms.AnimationType;
import org.envel.immersiveportalspaperized.bukkit.nms.EntityUtil;
import org.envel.immersiveportalspaperized.bukkit.nms.PacketUtil;
import org.envel.immersiveportalspaperized.bukkit.nms.RotationUtil;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
         PacketContainer spawnPacket = EntityUtil.getRawEntitySpawnPacket(tracker.getEntity());
         if (spawnPacket == null) {
            return;
         }

         if (spawnPacket.getUUIDs().size() > 0) {
            spawnPacket.getUUIDs().write(0, tracker.getEntityUniqueId());
         }

         spawnPacket.getIntegers().write(0, tracker.getEntityId());
         Vector actualPos = tracker.getEntity().getLocation().toVector();
         if (tracker.getEntity() instanceof Hanging) {
            actualPos = MathUtil.moveToCenterOfBlock(actualPos);
         }

         Vector renderedPos = tracker.getTranslation().transform(actualPos);
         PacketUtil.writeDoublePosition(spawnPacket, renderedPos);
         this.setSpawnRotation(spawnPacket, tracker);
         PacketUtil.sendPacket(players, spawnPacket);
         if (tracker.getEntity() instanceof LivingEntity) {
            EntityEquipmentWatcher equipmentWatcher = new EntityEquipmentWatcher((LivingEntity)tracker.getEntity());
            Map<ItemSlot, ItemStack> changes = equipmentWatcher.checkForChanges();
            if (changes.size() > 0) {
               this.sendEntityEquipment(tracker, changes, players);
            }
         }

         this.sendMetadata(tracker, players);
      } catch (Throwable e) {
         this.logger.finer("showEntity failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   private void setSpawnRotation(PacketContainer packet, EntityInfo entityInfo) {
      Location renderedPos = entityInfo.findRenderedLocation();
      int yaw = RotationUtil.getPacketRotationInt(renderedPos.getYaw());
      int pitch = RotationUtil.getPacketRotationInt(renderedPos.getPitch());
      PacketType packetType = packet.getType();
      if (packetType == Server.SPAWN_ENTITY) {
         if (entityInfo.getEntity() instanceof Hanging) {
            Direction currentDirection = RotationUtil.getDirection((Integer)packet.getIntegers().read(4));
            if (currentDirection != null) {
               Direction rotated = RotationUtil.rotateBy(currentDirection, entityInfo.getRotation());
               if (rotated == null) {
                  throw new IllegalStateException("Portal attempted to rotate a hanging entity to an invalid block direction");
               }

               packet.getIntegers().write(4, RotationUtil.getId(rotated));
            }
         }

         packet.getBytes().write(0, (byte)pitch);
         packet.getBytes().write(1, (byte)yaw);
      } else if (packetType == Server.NAMED_ENTITY_SPAWN) {
         packet.getBytes().write(0, (byte)yaw);
         packet.getBytes().write(1, (byte)pitch);
      }
   }

   @Override
   public void hideEntity(EntityInfo tracker, Collection<Player> players) {
      try {
         PacketContainer packet = new PacketContainer(Server.ENTITY_DESTROY);
         packet.getIntLists().write(0, Collections.singletonList(tracker.getEntityId()));
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("hideEntity failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityMove(EntityInfo tracker, Vector offset, Collection<Player> players) {
      try {
         offset = tracker.getRotation().transform(offset);
         PacketContainer packet = new PacketContainer(Server.REL_ENTITY_MOVE);
         packet.getIntegers().write(0, tracker.getEntityId());
         PacketUtil.writeRelativeOffset(packet, offset);
         packet.getBooleans().write(0, tracker.getEntity().isOnGround());
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
         PacketContainer packet = new PacketContainer(Server.REL_ENTITY_MOVE_LOOK);
         packet.getIntegers().write(0, tracker.getEntityId());
         PacketUtil.writeLookRotation(packet, entityPos.getYaw(), entityPos.getPitch());
         PacketUtil.writeRelativeOffset(packet, offset);
         packet.getBooleans().write(0, tracker.getEntity().isOnGround());
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityMoveLook failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityLook(EntityInfo tracker, Collection<Player> players) {
      try {
         Location entityPos = tracker.findRenderedLocation();
         PacketContainer packet = new PacketContainer(Server.ENTITY_LOOK);
         packet.getIntegers().write(0, tracker.getEntityId());
         PacketUtil.writeLookRotation(packet, entityPos.getYaw(), entityPos.getPitch());
         packet.getBooleans().write(0, tracker.getEntity().isOnGround());
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityLook failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityTeleport(EntityInfo tracker, Collection<Player> players) {
      try {
         PacketContainer packet = new PacketContainer(Server.ENTITY_TELEPORT);
         packet.getIntegers().write(0, tracker.getEntityId());
         Location entityPos = tracker.findRenderedLocation();
         PacketUtil.writeDoublePosition(packet, entityPos.toVector());
         PacketUtil.writeTeleportRotation(packet, entityPos.getYaw(), entityPos.getPitch());
         packet.getBooleans().write(0, tracker.getEntity().isOnGround());
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityTeleport failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityHeadRotation(EntityInfo tracker, Collection<Player> players) {
      try {
         Location renderedPos = tracker.findRenderedLocation();
         byte headRotation = RotationUtil.getPacketRotationByte(renderedPos.getYaw());

         PacketContainer packet;
         try {
            Object nmsEntity = tracker.getEntity().getClass().getMethod("getHandle").invoke(tracker.getEntity());
            Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
            Constructor<?> constructor = packetClass.getConstructor(nmsEntityClass, byte.class);
            Object nmsPacket = constructor.newInstance(nmsEntity, headRotation);
            packet = new PacketContainer(Server.ENTITY_HEAD_ROTATION, nmsPacket);
            packet.getIntegers().write(0, tracker.getEntityId());
         } catch (Throwable inner) {
            packet = new PacketContainer(Server.ENTITY_HEAD_ROTATION);
            packet.getIntegers().write(0, tracker.getEntityId());
            packet.getBytes().write(0, headRotation);
         }

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

         PacketContainer packet;
         try {
            Object nmsEntity = tracker.getEntity().getClass().getMethod("getHandle").invoke(tracker.getEntity());
            Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPassengersPacket");
            Constructor<?> constructor = packetClass.getConstructor(nmsEntityClass);
            Object nmsPacket = constructor.newInstance(nmsEntity);
            packet = new PacketContainer(Server.MOUNT, nmsPacket);
            packet.getIntegers().write(0, tracker.getEntityId());
            packet.getIntegerArrays().write(0, ridingIds);
         } catch (Throwable inner) {
            packet = new PacketContainer(Server.MOUNT);
            packet.getIntegers().write(0, tracker.getEntityId());
            packet.getIntegerArrays().write(0, ridingIds);
         }

         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendMount failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityEquipment(EntityInfo tracker, Map<ItemSlot, ItemStack> changes, Collection<Player> players) {
      try {
         List<Pair<ItemSlot, ItemStack>> wrappedChanges = new ArrayList<>();
         changes.forEach((slot, item) -> wrappedChanges.add(new Pair(slot, item == null ? new ItemStack(Material.AIR) : item)));
         PacketContainer packet = new PacketContainer(Server.ENTITY_EQUIPMENT);
         packet.getIntegers().write(0, tracker.getEntityId());
         packet.getSlotStackPairLists().write(0, wrappedChanges);
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityEquipment failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendMetadata(EntityInfo tracker, Collection<Player> players) {
      try {
         PacketContainer packet = new PacketContainer(Server.ENTITY_METADATA);
         packet.getIntegers().write(0, tracker.getEntityId());
         WrappedDataWatcher dataWatcher = EntityUtil.getActualDataWatcher(tracker.getEntity());
         List<WrappedDataValue> wrappedDataValueList = dataWatcher.getWatchableObjects().stream().filter(Objects::nonNull).map(entry -> {
            WrappedDataWatcherObject dataWatcherObject = entry.getWatcherObject();
            return new WrappedDataValue(dataWatcherObject.getIndex(), dataWatcherObject.getSerializer(), entry.getRawValue());
         }).toList();
         packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);
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
         PacketContainer packet = new PacketContainer(Server.ENTITY_VELOCITY);
         packet.getIntegers().write(0, tracker.getEntityId());
         PacketUtil.writeVelocity(packet, entityVelocity);
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityVelocity failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityAnimation(EntityInfo tracker, Collection<Player> players, AnimationType animationType) {
      try {
         PacketContainer packet;
         try {
            Object nmsEntity = tracker.getEntity().getClass().getMethod("getHandle").invoke(tracker.getEntity());
            Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAnimatePacket");
            Constructor<?> constructor = packetClass.getConstructor(nmsEntityClass, int.class);
            Object nmsPacket = constructor.newInstance(nmsEntity, animationType.getNmsId());
            packet = new PacketContainer(Server.ANIMATION, nmsPacket);
            packet.getIntegers().write(0, tracker.getEntityId());
         } catch (Throwable inner) {
            try {
               packet = new PacketContainer(Server.ANIMATION);
               StructureModifier<Integer> integers = packet.getIntegers();
               integers.write(0, tracker.getEntityId());
               integers.write(1, animationType.getNmsId());
            } catch (Throwable innerFallback) {
               this.logger.finer("sendEntityAnimation failed for entity %s: %s", tracker.getEntityId(), innerFallback.getMessage());
               return;
            }
         }

         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityAnimation failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendEntityPickupItem(EntityInfo tracker, EntityInfo pickedUp, Collection<Player> players) {
      try {
         PacketContainer packet = new PacketContainer(Server.COLLECT);
         StructureModifier<Integer> integers = packet.getIntegers();
         integers.write(0, pickedUp.getEntityId());
         integers.write(1, tracker.getEntityId());
         integers.write(2, ((Item)pickedUp.getEntity()).getItemStack().getAmount());
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendEntityPickupItem failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   private PlayerInfoData generatePlayerInfoData(EntityInfo tracker) {
      WrappedGameProfile profile = new WrappedGameProfile(tracker.getEntityUniqueId(), tracker.getEntity().getName());
      Player trackingPlayer = (Player)tracker.getEntity();
      WrappedGameProfile playerProfile = WrappedGameProfile.fromPlayer(trackingPlayer);

      try {
         Object profileHandle = profile.getHandle();
         Object playerProfileHandle = playerProfile.getHandle();
         Object properties = profileHandle.getClass().getMethod("getProperties").invoke(profileHandle);
         Object playerProperties = playerProfileHandle.getClass().getMethod("getProperties").invoke(playerProfileHandle);
         properties.getClass().getMethod("removeAll", Object.class).invoke(properties, "textures");
         Collection<?> textures = (Collection<?>)playerProperties.getClass().getMethod("get", Object.class).invoke(playerProperties, "textures");
         properties.getClass().getMethod("putAll", Object.class, Iterable.class).invoke(properties, "textures", textures);
      } catch (Throwable inner) {
         try {
            profile.getProperties().removeAll("textures");
            profile.getProperties().putAll("textures", playerProfile.getProperties().get("textures"));
         } catch (Throwable innerFallback) {
            this.logger.finer("Failed to copy player skin textures for %s: %s", trackingPlayer.getName(), innerFallback.getMessage());
         }
      }

      return new PlayerInfoData(
         profile, trackingPlayer.getPing(), NativeGameMode.fromBukkit(trackingPlayer.getGameMode()), WrappedChatComponent.fromText(trackingPlayer.getName())
      );
   }

   @Override
   public void sendAddPlayerProfile(EntityInfo tracker, Collection<Player> players) {
      try {
         PacketContainer packet = new PacketContainer(Server.PLAYER_INFO);
         packet.getPlayerInfoActions().write(0, Set.of(PlayerInfoAction.ADD_PLAYER));
         List<PlayerInfoData> playerInfoDataList = new ArrayList<>();
         playerInfoDataList.add(this.generatePlayerInfoData(tracker));
         packet.getPlayerInfoDataLists().write(1, playerInfoDataList);
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendAddPlayerProfile failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }

   @Override
   public void sendRemovePlayerProfile(EntityInfo tracker, Collection<Player> players) {
      try {
         PacketContainer packet = new PacketContainer(Server.PLAYER_INFO_REMOVE);
         packet.getUUIDLists().write(0, Collections.singletonList(tracker.getEntityUniqueId()));
         PacketUtil.sendPacket(players, packet);
      } catch (Throwable e) {
         this.logger.finer("sendRemovePlayerProfile failed for entity %s: %s", tracker.getEntityId(), e.getMessage());
      }
   }
}
