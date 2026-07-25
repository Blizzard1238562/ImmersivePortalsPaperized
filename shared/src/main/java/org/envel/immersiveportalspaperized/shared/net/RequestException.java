package org.envel.immersiveportalspaperized.shared.net;

public class RequestException extends Exception {
   private static final long serialVersionUID = 1L;

   public RequestException(String message) {
      super(message);
   }

   public RequestException(Throwable cause) {
      super(cause);
   }

   public RequestException(Throwable cause, String message) {
      super(message, cause);
   }
}
