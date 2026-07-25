package org.envel.immersiveportalspaperized.shared.util;

public class ReflectionException extends RuntimeException {
   public ReflectionException(ReflectiveOperationException cause) {
      super(cause);
   }

   public ReflectionException(String message, Throwable cause) {
      super(message, cause);
   }

   public ReflectionException(String message) {
      super(message);
   }
}
