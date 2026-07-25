package org.envel.immersiveportalspaperized.shared.logging;

public abstract class Logger extends java.util.logging.Logger {
   protected Logger(String name, String resourceBundleName) {
      super(name, resourceBundleName);
   }

   public void severe(String format, Object... args) {
      super.severe(String.format(format, args));
   }

   public void warning(String format, Object... args) {
      super.warning(String.format(format, args));
   }

   public void info(String format, Object... args) {
      super.info(String.format(format, args));
   }

   public void fine(String format, Object... args) {
      super.fine(String.format(format, args));
   }

   public void finer(String format, Object... args) {
      super.finer(String.format(format, args));
   }

   public void finest(String format, Object... args) {
      super.finest(String.format(format, args));
   }
}
