package org.envel.immersiveportalspaperized.shared.logging;

import java.util.logging.Level;

public abstract class Logger extends java.util.logging.Logger {
   protected Logger(String name, String resourceBundleName) {
      super(name, resourceBundleName);
   }

   public void severe(String format, Object... args) {
      if (this.isLoggable(Level.SEVERE)) {
         super.severe(String.format(format, args));
      }
   }

   public void warning(String format, Object... args) {
      if (this.isLoggable(Level.WARNING)) {
         super.warning(String.format(format, args));
      }
   }

   public void info(String format, Object... args) {
      if (this.isLoggable(Level.INFO)) {
         super.info(String.format(format, args));
      }
   }

   public void fine(String format, Object... args) {
      if (this.isLoggable(Level.FINE)) {
         super.fine(String.format(format, args));
      }
   }

   public void finer(String format, Object... args) {
      if (this.isLoggable(Level.FINER)) {
         super.finer(String.format(format, args));
      }
   }

   public void finest(String format, Object... args) {
      if (this.isLoggable(Level.FINEST)) {
         super.finest(String.format(format, args));
      }
   }
}
