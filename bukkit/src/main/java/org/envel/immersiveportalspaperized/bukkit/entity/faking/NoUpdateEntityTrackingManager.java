package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class NoUpdateEntityTrackingManager extends EntityTrackingManager {
   @Inject
   public NoUpdateEntityTrackingManager(Logger logger, IEntityTracker.Factory entityTrackerFactory) {
      super(logger, entityTrackerFactory);
   }

   @Override
   public void update() {
   }
}
