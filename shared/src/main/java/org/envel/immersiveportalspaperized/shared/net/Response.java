package org.envel.immersiveportalspaperized.shared.net;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * Serializable response envelope for cross-server requests.
 * <p>
 * Either {@link #result} or {@link #error} will be populated, never both.
 * </p>
 */
public class Response implements Serializable {
   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private int id;

   @Setter
   private Object result;

   @Setter
   private RequestException error;

   public Object getResult() throws RequestException {
      this.checkForErrors();
      return this.result;
   }

   public void checkForErrors() throws RequestException {
      if (this.error != null) {
         throw this.error;
      }
   }
}
