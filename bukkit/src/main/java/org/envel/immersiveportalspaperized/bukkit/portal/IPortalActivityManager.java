package org.envel.immersiveportalspaperized.bukkit.portal;

/**
 * IPortalActivityManager.
 */
public interface IPortalActivityManager {
   void onPortalViewedThisTick(IPortal var1);

   void onPortalActivatedThisTick(IPortal var1);

   void postUpdate();

   void resetActivity();
}


