package org.aktin.broker.auth.otp.utils;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * A {@link PasswordHasher} implementation that uses the PBKDF2WithHmacSHA256 algorithm.
 * <p>
 * The generated hash string is formatted as {@code alg:iter:salt:hash} and includes all necessary parameters for verification. The verification process uses a constant-time comparison to mitigate
 * timing attacks.
 */
public class Pbkdf2PasswordHasher implements PasswordHasher {

  private static final SecureRandom RNG = new SecureRandom();
  private static final String ALG = "pbkdf2";
  private static final String JCA = "PBKDF2WithHmacSHA256";
  private static final int SALT_BYTES = 16;
  private static final int ITER = 120_000;
  private static final int DK_LEN = 32; // 256-bit

  @Override
  public String hash(char[] password) {
    byte[] salt = new byte[SALT_BYTES];
    RNG.nextBytes(salt);
    byte[] dk = derive(password, salt, ITER, DK_LEN);
    return ALG + ":" + ITER + ":" + b64(salt) + ":" + b64(dk);
  }

  @Override
  public boolean verify(char[] password, String stored) {
    try {
      String[] p = stored.split(":");
      if (p.length != 4 || !ALG.equals(p[0])) {
        return false;
      }
      int iter = Integer.parseInt(p[1]);
      byte[] salt = b64d(p[2]);
      byte[] expected = b64d(p[3]);
      byte[] dk = derive(password, salt, iter, expected.length);
      // constant-time compare
      if (dk.length != expected.length) {
        return false;
      }
      int r = 0;
      for (int i = 0; i < dk.length; i++) {
        r |= (dk[i] ^ expected[i]);
      }
      return r == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private static byte[] derive(char[] pwd, byte[] salt, int iter, int dkLen) {
    try {
      var spec = new PBEKeySpec(pwd, salt, iter, dkLen * 8);
      return SecretKeyFactory.getInstance(JCA).generateSecret(spec).getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException("PBKDF2 failed", e);
    }
  }

  private static String b64(byte[] b) {
    return Base64.getEncoder().encodeToString(b);
  }

  private static byte[] b64d(String s) {
    return Base64.getDecoder().decode(s);
  }
}
