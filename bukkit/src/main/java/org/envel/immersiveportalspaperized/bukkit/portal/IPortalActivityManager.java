package org.envel.immersiveportalspaperized.bukkit.portal;

public interface IPortalActivityManager {
   void onPortalViewedThisTick(IPortal var1);

   void onPortalActivatedThisTick(IPortal var1);

   void postUpdate();

   void resetActivity();
}
