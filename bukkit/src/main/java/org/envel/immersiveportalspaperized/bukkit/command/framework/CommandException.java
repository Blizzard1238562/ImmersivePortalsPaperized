package org.envel.immersiveportalspaperized.bukkit.command.framework;

public class CommandException extends Exception {
   public CommandException(String message) {
      super(message);
   }

   public CommandException(Throwable cause) {
      super(cause);
   }

   public CommandException(String message, Throwable cause) {
      super(message, cause);
   }
}
