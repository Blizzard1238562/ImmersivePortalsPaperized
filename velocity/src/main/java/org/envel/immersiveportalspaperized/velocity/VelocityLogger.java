package org.envel.immersiveportalspaperized.velocity;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class VelocityLogger extends Logger {
   private final org.slf4j.Logger logger;

   protected VelocityLogger(org.slf4j.Logger logger) {
      super(logger.getName(), null);
      this.setLevel(Level.INFO);
      this.logger = logger;
   }

   @Override
   public void log(LogRecord record) {
      if (record.getLevel().intValue() >= super.getLevel().intValue()) {
         int recordLevel = record.getLevel().intValue();
         if (recordLevel >= Level.SEVERE.intValue()) {
            this.logger.error("{}", record.getMessage());
         } else if (recordLevel >= Level.WARNING.intValue()) {
            this.logger.warn("{}", record.getMessage());
         } else if (recordLevel >= Level.INFO.intValue()) {
            this.logger.info("{}", record.getMessage());
         } else if (recordLevel >= Level.FINE.intValue()) {
            this.logger.info("[FNE] {}", record.getMessage());
         } else if (recordLevel >= Level.FINER.intValue()) {
            this.logger.info("[FNR] {}", record.getMessage());
         } else if (recordLevel >= Level.FINEST.intValue()) {
            this.logger.info("[FST] {}", record.getMessage());
         }
      }
   }
}
