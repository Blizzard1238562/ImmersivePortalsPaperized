package org.envel.immersiveportalspaperized.shared.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class OverrideLogger extends Logger {
   private static final Map<Level, String> logLevelNames = new HashMap<>();
   private final java.util.logging.Logger logger;

   public OverrideLogger(java.util.logging.Logger logger) {
      super(logger.getName(), null);
      super.setLevel(Level.INFO);
      this.logger = logger;
   }

   @Override
   public void log(LogRecord record) {
      if (record.getLevel().intValue() >= super.getLevel().intValue()) {
         Level originalLevel = record.getLevel();
         if (originalLevel.intValue() < Level.INFO.intValue()) {
            record.setLevel(Level.INFO);
            String levelName = logLevelNames.get(originalLevel);
            if (levelName == null) {
               levelName = originalLevel.getName();
            }

            record.setMessage(String.format("[%s] %s", levelName, record.getMessage()));
         }

         this.logger.log(record);
      }
   }

   static {
      logLevelNames.put(Level.SEVERE, "SEV");
      logLevelNames.put(Level.WARNING, "WRN");
      logLevelNames.put(Level.INFO, "INF");
      logLevelNames.put(Level.CONFIG, "CFG");
      logLevelNames.put(Level.FINE, "FNE");
      logLevelNames.put(Level.FINER, "FNR");
      logLevelNames.put(Level.FINEST, "FST");
   }
}
