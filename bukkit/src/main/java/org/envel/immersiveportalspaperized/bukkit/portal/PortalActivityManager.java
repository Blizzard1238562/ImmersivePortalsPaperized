package org.envel.immersiveportalspaperized.bukkit.portal;

import java.util.HashSet;
import java.util.Set;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * PortalActivityManager.
 */
@Singleton
public class PortalActivityManager implements IPortalActivityManager {
   private final Logger logger;
   private final Set<IPortal> activePortals = new HashSet<>();
   private final Set<IPortal> activePortalsYetToUpdate = new HashSet<>();
   private final Set<IPortal> viewedPortals = new HashSet<>();
   private final Set<IPortal> viewActivePortalsYetToUpdate = new HashSet<>();

   @Inject
   public PortalActivityManager(Logger logger) {
      this.logger = logger;
   }

   @Override
   public synchronized void onPortalActivatedThisTick(IPortal portal) {
      if (!this.activePortals.contains(portal)) {
         portal.onActivate();
         this.activePortals.add(portal);
         this.activePortalsYetToUpdate.add(portal);
      }

      if (this.activePortalsYetToUpdate.remove(portal)) {
         portal.onUpdate();
      }
   }

   @Override
   public synchronized void onPortalViewedThisTick(IPortal portal) {
      if (!this.viewedPortals.contains(portal)) {
         portal.onViewActivate();
         this.viewedPortals.add(portal);
         this.viewActivePortalsYetToUpdate.add(portal);
      }

      if (this.viewActivePortalsYetToUpdate.remove(portal)) {
         portal.onViewUpdate();
      }
   }

   @Override
   public synchronized void postUpdate() {
      for (IPortal portal : this.viewActivePortalsYetToUpdate) {
         this.viewedPortals.remove(portal);
         portal.onViewDeactivate();
      }

      this.viewActivePortalsYetToUpdate.clear();
      this.viewActivePortalsYetToUpdate.addAll(this.viewedPortals);

      for (IPortal portal : this.activePortalsYetToUpdate) {
         this.activePortals.remove(portal);
         portal.onDeactivate();
      }

      this.activePortalsYetToUpdate.clear();
      this.activePortalsYetToUpdate.addAll(this.activePortals);
   }

   @Override
   public synchronized void resetActivity() {
      for (IPortal portal : this.activePortals) {
         portal.onDeactivate();
         if (this.viewedPortals.contains(portal)) {
            portal.onViewDeactivate();
         }
      }

      this.activePortals.clear();
      this.viewedPortals.clear();
      this.activePortalsYetToUpdate.clear();
      this.viewActivePortalsYetToUpdate.clear();
   }
}


