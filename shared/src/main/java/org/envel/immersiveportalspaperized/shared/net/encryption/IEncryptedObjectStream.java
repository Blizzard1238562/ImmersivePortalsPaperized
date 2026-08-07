package org.envel.immersiveportalspaperized.shared.net.encryption;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Object stream abstraction that encrypts and decrypts Java objects over a socket.
 */
public interface IEncryptedObjectStream {
   int MAX_REQUEST_SIZE = 31457280;

   Object readObject() throws GeneralSecurityException, IOException, ClassNotFoundException;

   void writeObject(Object obj) throws GeneralSecurityException, IOException;
}
