package org.envel.immersiveportalspaperized.bukkit.portal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.bukkit.block.IBlockMap;
import org.envel.immersiveportalspaperized.bukkit.chunk.chunkloading.PortalChunkLoader;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.entity.IPortalEntityManager;
import org.envel.immersiveportalspaperized.bukkit.math.PortalTransformations;
import org.envel.immersiveportalspaperized.bukkit.math.PortalTransformationsFactory;
import org.envel.immersiveportalspaperized.bukkit.util.MaterialUtil;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * Portal.
 */
public class Portal implements IPortal, ConfigurationSerializable {
   @Getter
   private final UUID id;
   @Getter
   private final UUID ownerId;
   @Getter
   private String name;
   private final IPortalManager portalManager;
   private final Logger logger;
   @Getter
   private final PortalPosition originPos;
   @Getter
   private final PortalPosition destPos;
   @Getter
   private final Vector size;
   @Getter
   private final boolean isCrossServer;
   @Getter
   private final boolean isCustom;
   private boolean allowNonPlayerTeleportation;
   @Getter
   private final PortalTransformations transformations;
   @Getter
   private final IBlockMap viewableBlocks;
   @Getter
   private final IPortalEntityManager entityList;
   private final PortalChunkLoader chunkLoader;
   private int ticksSinceActivated = -1;
   private int ticksSinceViewActivated = -1;
   private boolean originBlockValidCached = true;
   private boolean destBlockValidCached = true;
   private final AtomicBoolean originChecking = new AtomicBoolean(false);
   private final AtomicBoolean destChecking = new AtomicBoolean(false);
   @Getter
   @Setter
   private double price = 0.0;
   @Nullable
   @Getter
   @Setter
   private String effectPreset = null;
   @Getter
   @Setter
   private boolean soundEnabled = true;
   @Inject
   private static IPortal.Factory deserializationFactory;

   @Inject
   public Portal(
      IPortalManager portalManager,
      IPortalEntityManager.Factory entityListFactory,
      IBlockMap.Factory viewableBlockArrayFactory,
      PortalChunkLoader chunkLoader,
      MiscConfig miscConfig,
      Logger logger,
      PortalTransformationsFactory transformationsFactory,
      @Assisted("originPos") PortalPosition originPos,
      @Assisted("destPos") PortalPosition destPos,
      @Assisted Vector size,
      @Assisted("isCustom") boolean isCustom,
      @Assisted("id") UUID id,
      @Nullable @Assisted("ownerId") UUID ownerId,
      @Nullable @Assisted("name") String name,
      @Assisted("allowNonPlayerTeleportation") boolean allowNonPlayerTeleportation
   ) {
      this.portalManager = portalManager;
      this.logger = logger;
      this.originPos = originPos;
      this.destPos = destPos;
      this.size = size;
      this.isCrossServer = destPos.isExternal();
      this.isCustom = isCustom;
      this.allowNonPlayerTeleportation = allowNonPlayerTeleportation;
      this.entityList = entityListFactory.create(this, !this.isCrossServer && miscConfig.isEntitySupportEnabled());
      this.chunkLoader = chunkLoader;
      this.id = id;
      this.ownerId = ownerId;
      this.name = name;
      this.transformations = transformationsFactory.create(this);
      this.viewableBlocks = viewableBlockArrayFactory.create(this);
   }

   @Override
   public void onUpdate() {
      if (!this.isStillValid()) {
         this.remove(true);
      }

      this.entityList.update(this.ticksSinceActivated);
      this.ticksSinceActivated++;
   }

   @Override
   public void onViewUpdate() {
      this.viewableBlocks.update(this.ticksSinceViewActivated);
      this.ticksSinceViewActivated++;
   }

   @Override
   public void onActivate() {
      this.logger.finer("Portal was activated");
      this.chunkLoader.forceloadPortalChunks(this.destPos);
      this.ticksSinceActivated = 0;
   }

   @Override
   public void onDeactivate() {
      this.logger.finer("Portal was deactivated");
      this.chunkLoader.unforceloadPortalChunks(this.destPos);
      this.viewableBlocks.reset();
      this.ticksSinceActivated = -1;
   }

   @Override
   public void onViewActivate() {
      this.logger.finest("Portal was view-activated");
      this.ticksSinceViewActivated = 0;
   }

   @Override
   public void onViewDeactivate() {
      this.logger.finest("Portal was view-deactivated");
      this.ticksSinceViewActivated = -1;
   }

   @Override
   public void remove(boolean removeOtherDirection) {
      this.portalManager.removePortal(this);
      // NOTE: onDeactivate() normally resets the block map's internal state (nonObscuredStates,
      // tile entity maps, stateQueue) when a portal stops being active, to free the memory - but
      // remove() (actual portal destruction) never called it, so that state stayed allocated on
      // the now-orphaned Portal object. Per-player ghost-block cleanup itself doesn't depend on
      // this (PlayerBlockStates tracks what it showed per-player independently and resets it once
      // PlayerData notices the portal is no longer viewable), but resetting here matches
      // onDeactivate()'s behavior and avoids leaking the block-map state.
      this.viewableBlocks.reset();
      this.originPos.getLocation().getBlock().setType(Material.AIR);
      if (removeOtherDirection) {
         this.portalManager.removePortalsAt(this.destPos.getLocation());
         if (this.isNetherPortal()) {
            this.destPos.getLocation().getBlock().setType(Material.AIR);
         }
      }
   }

   @Override
   public String getPermissionPath() {
      if (this.isCustom) {
         return this.getName() != null ? ".custom." + this.getName() : "";
      } else {
         return ".nether." + this.originPos.getWorldName();
      }
   }

   @Override
   public boolean isRegistered() {
      return this.portalManager.getPortalById(this.id) != null;
   }

   @Override
   public void setName(@Nullable String newName) {
      if (this.isNetherPortal()) {
         throw new IllegalStateException("Cannot set name of nether portal");
      } else {
         this.name = newName;
      }
   }

   @Override
   public boolean allowsNonPlayerTeleportation() {
      return this.allowNonPlayerTeleportation;
   }

   @Override
   public void setAllowsNonPlayerTeleportation(boolean allow) {
      this.allowNonPlayerTeleportation = allow;
   }

   private void updateValidityCache() {
      if (SchedulerUtil.isFolia() && !this.isCustom) {
         if (this.originChecking.compareAndSet(false, true)) {
            Location loc = this.originPos.getLocation();
            World world = loc.getWorld();
            if (world != null) {
               SchedulerUtil.runAtLocation(
                  loc,
                  () -> {
                     try {
                        this.originBlockValidCached = world.getBlockData(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()).getMaterial()
                           == MaterialUtil.PORTAL_MATERIAL;
                     } catch (Exception var7) {
                     } finally {
                        this.originChecking.set(false);
                     }
                  }
               );
            } else {
               this.originChecking.set(false);
            }
         }

         if (!this.isCrossServer && this.destChecking.compareAndSet(false, true)) {
            Location loc = this.destPos.getLocation();
            World world = loc.getWorld();
            if (world != null) {
               SchedulerUtil.runAtLocation(
                  loc,
                  () -> {
                     try {
                        this.destBlockValidCached = world.getBlockData(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()).getMaterial()
                           == MaterialUtil.PORTAL_MATERIAL;
                     } catch (Exception var7) {
                     } finally {
                        this.destChecking.set(false);
                     }
                  }
               );
            } else {
               this.destChecking.set(false);
            }
         }
      }
   }

   private boolean isStillValid() {
      if (this.isCustom) {
         return true;
      } else if (SchedulerUtil.isFolia()) {
         this.updateValidityCache();
         return this.originBlockValidCached && this.destBlockValidCached;
      } else {
         return this.originPos.getLocation().getBlock().getType() == MaterialUtil.PORTAL_MATERIAL
            && (this.isCrossServer || this.destPos.getLocation().getBlock().getType() == MaterialUtil.PORTAL_MATERIAL);
      }
   }

   @NotNull
   public Map<String, Object> serialize() {
      Map<String, Object> result = new HashMap<>();
      result.put("originPos", this.originPos);
      result.put("destPos", this.destPos);
      result.put("size", this.size);
      result.put("anchored", this.isCustom);
      result.put("id", this.id.toString());
      result.put("allowsNonPlayerTeleportation", this.allowsNonPlayerTeleportation());
      if (this.ownerId != null) {
         result.put("owner", this.ownerId.toString());
      }

      if (this.name != null) {
         result.put("name", this.name);
      }

      result.put("price", this.price);
      if (this.effectPreset != null) {
         result.put("effectPreset", this.effectPreset);
      }

      result.put("soundEnabled", this.soundEnabled);
      return result;
   }

   public static Portal valueOf(Map<String, Object> map) {
      String idString = (String)map.get("id");
      UUID id = idString == null ? UUID.randomUUID() : UUID.fromString(idString);
      String ownerIdString = (String)map.get("owner");
      UUID ownerId = ownerIdString == null ? null : UUID.fromString(ownerIdString);
      Portal portal = (Portal)deserializationFactory.create(
         (PortalPosition)map.get("originPos"),
         (PortalPosition)map.get("destPos"),
         (Vector)map.get("size"),
         (Boolean)map.get("anchored"),
         id,
         ownerId,
         (String)map.get("name"),
         (Boolean)map.getOrDefault("allowsNonPlayerTeleportation", true)
      );
      if (map.containsKey("price")) {
         portal.setPrice(((Number)map.get("price")).doubleValue());
      }

      if (map.containsKey("effectPreset")) {
         portal.setEffectPreset((String)map.get("effectPreset"));
      }

      if (map.containsKey("soundEnabled")) {
         portal.setSoundEnabled((Boolean)map.get("soundEnabled"));
      }

      return portal;
   }
}


