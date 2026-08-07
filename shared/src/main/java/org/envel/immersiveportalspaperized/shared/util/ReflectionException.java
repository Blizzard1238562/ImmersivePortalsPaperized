package org.envel.immersiveportalspaperized.shared.util;

/**
 * Wraps reflection-related failures encountered by {@link ReflectionUtil}.
 */
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
