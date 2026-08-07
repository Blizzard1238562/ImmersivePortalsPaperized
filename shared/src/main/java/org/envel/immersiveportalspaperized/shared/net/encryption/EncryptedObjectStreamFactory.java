package org.envel.immersiveportalspaperized.shared.net.encryption;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

/**
 * Factory for creating {@link IEncryptedObjectStream} instances from raw streams.
 */
public interface EncryptedObjectStreamFactory {
   IEncryptedObjectStream create(InputStream inputStream, OutputStream outputStream) throws GeneralSecurityException;
}
