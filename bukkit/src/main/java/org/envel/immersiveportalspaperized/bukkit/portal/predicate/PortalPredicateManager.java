package org.envel.immersiveportalspaperized.bukkit.portal.predicate;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class PortalPredicateManager implements IPortalPredicateManager {
   private final Logger logger;
   private final List<PortalPredicate> activationPredicates = new ArrayList<>();
   private final List<PortalPredicate> viewPredicates = new ArrayList<>();
   private final List<PortalPredicate> teleportationPredicates = new ArrayList<>();

   @Inject
   public PortalPredicateManager(
      Logger logger,
      IPlayerDataManager playerDataManager,
      ActivationDistance activationDistance,
      CrossServerDestinationChecker crossServerDestinationChecker,
      EconomyChargeChecker economyChargeChecker,
      TeleportCooldownChecker teleportCooldownChecker
   ) {
      this.logger = logger;
      this.addActivationPredicate(activationDistance);
      this.addActivationPredicate(crossServerDestinationChecker);
      this.addViewPredicate(new PermissionsChecker("immersiveportalspaperized.see"));
      this.addViewPredicate(new PlayerPreferenceChecker(playerDataManager, "seeThroughPortal"));
      this.addTeleportPredicate(new PermissionsChecker("immersiveportalspaperized.user"));
      this.addTeleportPredicate(economyChargeChecker);
      this.addTeleportPredicate(teleportCooldownChecker);
   }

   @Override
   public void addActivationPredicate(PortalPredicate predicate) {
      this.logger.fine("Portal activation predicate added of type %s", predicate.getClass().getName());
      this.activationPredicates.add(predicate);
   }

   @Override
   public boolean removeActivationPredicate(PortalPredicate predicate) {
      this.logger.fine("Portal activation predicate removed of type %s", predicate.getClass().getName());
      return this.activationPredicates.remove(predicate);
   }

   @Override
   public void addViewPredicate(PortalPredicate predicate) {
      this.logger.fine("Portal view predicate added of type %s", predicate.getClass().getName());
      this.viewPredicates.add(predicate);
   }

   @Override
   public boolean removeViewPredicate(PortalPredicate predicate) {
      this.logger.fine("Portal view predicate removed of type %s", predicate.getClass().getName());
      return this.viewPredicates.remove(predicate);
   }

   @Override
   public void addTeleportPredicate(PortalPredicate predicate) {
      this.logger.fine("Portal teleportation predicate added of type %s", predicate.getClass().getName());
      this.teleportationPredicates.add(predicate);
   }

   @Override
   public boolean removeTeleportPredicate(PortalPredicate predicate) {
      this.logger.fine("Portal teleportation predicate removed of type %s", predicate.getClass().getName());
      return this.teleportationPredicates.remove(predicate);
   }

   @Override
   public boolean isActivatable(IPortal portal, Player player) {
      for (PortalPredicate predicate : this.activationPredicates) {
         if (!predicate.test(portal, player)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public boolean isViewable(IPortal portal, Player player) {
      for (PortalPredicate predicate : this.viewPredicates) {
         if (!predicate.test(portal, player)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public boolean canTeleport(IPortal portal, Player player) {
      for (PortalPredicate predicate : this.teleportationPredicates) {
         if (!predicate.test(portal, player)) {
            return false;
         }
      }

      return true;
   }
}
