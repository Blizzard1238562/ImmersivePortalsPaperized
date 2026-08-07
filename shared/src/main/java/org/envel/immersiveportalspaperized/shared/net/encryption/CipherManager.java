package org.envel.immersiveportalspaperized.shared.net.encryption;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import com.google.inject.Singleton;

/**
 * AES/GCM cipher lifecycle manager. Derives a 256-bit key from a shared UUID seed
 * and produces per-message encrypt/decrypt ciphers with random nonces.
 */
@Singleton
public class CipherManager {
   private static final int AES_KEY_SIZE = 256;
   public static final int GCM_NONCE_LENGTH = 12;
   private static final int GCM_TAG_LENGTH = 16;
   private SecretKey secretKey;
   private SecureRandom random;

   public void init(UUID key) throws NoSuchAlgorithmException {
      this.random = SecureRandom.getInstance("SHA1PRNG");
      byte[] nonce = new byte[GCM_NONCE_LENGTH];
      this.random.nextBytes(nonce);
      SecureRandom keyRandom = SecureRandom.getInstance("SHA1PRNG");
      keyRandom.setSeed(this.uuidToBytes(key));
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(AES_KEY_SIZE, keyRandom);
      this.secretKey = keyGenerator.generateKey();
   }

   private byte[] uuidToBytes(UUID id) {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
      buffer.putLong(id.getMostSignificantBits());
      buffer.putLong(id.getLeastSignificantBits());
      return buffer.array();
   }

   private byte[] generateRandomNonce() {
      byte[] nonce = new byte[GCM_NONCE_LENGTH];
      this.random.nextBytes(nonce);
      return nonce;
   }

   private GCMParameterSpec getGcmParameterSpec(byte[] nonce) {
      return new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
   }

   public Cipher createEncrypt() throws GeneralSecurityException {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, this.getGcmParameterSpec(this.generateRandomNonce()));
      return cipher;
   }

   public Cipher createDecrypt(byte[] nonce) throws GeneralSecurityException {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, this.secretKey, this.getGcmParameterSpec(nonce));
      return cipher;
   }
}
