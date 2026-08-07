package org.envel.immersiveportalspaperized.shared.net.requests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import lombok.Getter;
import lombok.Setter;

/**
 * Wraps an inner {@link Request} for forwarding through the proxy to another server.
 */
public class RelayRequest extends Request {
   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private String destination;

   private byte[] innerRequest;

   public void setInnerRequest(Request request) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      try {
         new ObjectOutputStream(outputStream).writeObject(request);
      } catch (IOException var4) {
      }

      this.innerRequest = outputStream.toByteArray();
   }

   public Request getInnerRequest() throws ClassNotFoundException, IOException {
      return (Request)new ObjectInputStream(new ByteArrayInputStream(this.innerRequest)).readObject();
   }
}
