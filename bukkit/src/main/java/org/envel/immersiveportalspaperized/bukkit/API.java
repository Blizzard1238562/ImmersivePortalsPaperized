package org.envel.immersiveportalspaperized.bukkit;

import java.util.UUID;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.ImmersivePortalsPaperizedAPI;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.api.UnknownPredicateException;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.IPortalPredicateManager;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * API.
 */
public class API extends ImmersivePortalsPaperizedAPI {
   private final Logger logger;
   private final IPortal.Factory portalFactory;
   private final IPortalManager portalManager;
   private final IPortalPredicateManager portalPredicateManager;

   @Inject
   public API(Logger logger, IPortal.Factory portalFactory, IPortalManager portalManager, IPortalPredicateManager portalPredicateManager) {
      this.logger = logger;
      this.portalFactory = portalFactory;
      this.portalManager = portalManager;
      this.portalPredicateManager = portalPredicateManager;
      this.onEnable();
   }

   public void onEnable() {
      this.logger.fine("Setting API instance");
      ImmersivePortalsPaperizedAPI.setInstance(this);
   }

   public void onDisable() {
      this.logger.fine("Removing API instance");
      ImmersivePortalsPaperizedAPI.setInstance(null);
   }

   private void verifyEnabled() {
      ImmersivePortalsPaperizedAPI.get();
   }

   @NotNull
   @Override
   public ImmersivePortal createPortal(
      @NotNull PortalPosition originPosition,
      @NotNull PortalPosition destinationPosition,
      @NotNull Vector size,
      @Nullable UUID owner,
      @Nullable String name,
      boolean isCustom
   ) {
      this.verifyEnabled();
      UUID id = UUID.randomUUID();
      IPortal portal = this.portalFactory.create(originPosition, destinationPosition, size, isCustom, id, owner, name, true);
      this.portalManager.registerPortal(portal);
      return new ApiPortalWrapper(id);
   }

   @Override
   public void addPortalActivationPredicate(@NotNull PortalPredicate predicate) {
      this.verifyEnabled();
      this.portalPredicateManager.addActivationPredicate(predicate);
   }

   @Override
   public void removePortalActivationPredicate(@NotNull PortalPredicate predicate) {
      this.verifyEnabled();
      if (!this.portalPredicateManager.removeActivationPredicate(predicate)) {
         throw new UnknownPredicateException(predicate);
      }
   }

   @Override
   public void addPortalViewPredicate(@NotNull PortalPredicate predicate) {
      this.verifyEnabled();
      this.portalPredicateManager.addViewPredicate(predicate);
   }

   @Override
   public void removePortalViewPredicate(@NotNull PortalPredicate predicate) {
      this.verifyEnabled();
      if (!this.portalPredicateManager.removeViewPredicate(predicate)) {
         throw new UnknownPredicateException(predicate);
      }
   }

   @Override
   public void addPortalTeleportPredicate(@NotNull PortalPredicate predicate) {
      this.verifyEnabled();
      this.portalPredicateManager.addTeleportPredicate(predicate);
   }

   @Override
   public void removePortalTeleportPredicate(@NotNull PortalPredicate predicate) {
      this.verifyEnabled();
      if (!this.portalPredicateManager.removeTeleportPredicate(predicate)) {
         throw new UnknownPredicateException(predicate);
      }
   }

   @Override
   public ImmersivePortal getPortalById(@NotNull UUID id) {
      this.verifyEnabled();
      ImmersivePortal portal = this.portalManager.getPortalById(id);
      return portal == null ? null : new ApiPortalWrapper(id);
   }

   public IPortalManager getPortalManager() {
      return this.portalManager;
   }
}


