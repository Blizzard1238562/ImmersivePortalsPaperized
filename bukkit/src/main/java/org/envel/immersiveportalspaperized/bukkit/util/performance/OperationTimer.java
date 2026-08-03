package org.envel.immersiveportalspaperized.bukkit.util.performance;

import java.time.Duration;
import java.time.Instant;

/**
 * OperationTimer.
 */
public class OperationTimer {
   private final Instant before = this.getNowPrecise();

   public Duration getTimeTaken() {
      return Duration.between(this.before, this.getNowPrecise());
   }

   public long getTimeTakenNanoSeconds() {
      return this.getTimeTaken().toNanos();
   }

   public double getTimeTakenSeconds() {
      return this.getTimeTakenNanoSeconds() / 1.0E9;
   }

   public double getTimeTakenMillis() {
      return this.getTimeTakenNanoSeconds() / 1000000.0;
   }

   private Instant getNowPrecise() {
      long timeNano = System.nanoTime();
      long leftOverNano = timeNano % 1000000000L;
      long seconds = (timeNano - leftOverNano) / 1000000000L;
      return Instant.ofEpochSecond(seconds, leftOverNano);
   }
}


