package games.strategy.engine.lobby.server.login;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import games.strategy.util.Util;

/**
 * A class which implements the TripleA-Lobby-Login authentication system using RSA encryption
 * for passwords.
 */
public class RsaAuthenticator {
  private static final Cache<String, PrivateKey> rsaKeyMap = buildCache();
  private static final String RSA = "RSA";
  private static final String RSA_ECB_OAEPP = RSA + "/ECB/OAEPPadding";
  private static final String PSEUDO_SALT = "TripleA";

  @VisibleForTesting
  static final String ENCRYPTED_PASSWORD_KEY = "RSAPWD";

  @VisibleForTesting
  static final String RSA_PUBLIC_KEY = "RSAPUBLICKEY";

  private static Cache<String, PrivateKey> buildCache() {
    return CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
  }

  /**
   * Returns true if the specified map contains the required values.
   */
  static boolean canProcessResponse(final Map<String, String> response) {
    return response.get(ENCRYPTED_PASSWORD_KEY) != null;
  }

  /**
   * Returns true if the specified map contains the required values.
   */
  public static boolean canProcessChallenge(final Map<String, String> challenge) {
    return challenge.get(RSA_PUBLIC_KEY) != null;
  }

  /**
   * Adds public key of a generated key-pair to the challenge map
   * and stores the private key in a map.
   */
  static Map<String, String> generatePublicKey() {
    return Collections.singletonMap(RSA_PUBLIC_KEY, storeKeyPair());
  }

  private static String storeKeyPair() {
    KeyPair keyPair;
    String publicKey;
    do {
      keyPair = generateKeyPair();
      publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    } while (rsaKeyMap.getIfPresent(publicKey) != null);
    rsaKeyMap.put(publicKey, keyPair.getPrivate());
    return publicKey;
  }

  private static KeyPair generateKeyPair() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
      keyGen.initialize(4096);
      return keyGen.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(RSA + " is an invalid algorithm!", e);
    }
  }

  private static String decryptPassword(final String encryptedPassword, final PrivateKey privateKey,
      final Function<String, String> successfullDecryptionAction) {
    Preconditions.checkNotNull(encryptedPassword);
    Preconditions.checkNotNull(privateKey);
    try {
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
      return successfullDecryptionAction
          .apply(new String(cipher.doFinal(Base64.getDecoder().decode(encryptedPassword)), StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException(e);
    } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      return e.getMessage();
    }
  }

  /**
   * Attempts to decrypt the given password using the challenge and response parameters.
   */
  public static String decryptPasswordForAction(final Map<String, String> challenge, final Map<String, String> response,
      final Function<String, String> successFullDecryptionAction) {
    final String publicKey = challenge.get(RSA_PUBLIC_KEY);
    final PrivateKey privateKey = rsaKeyMap.getIfPresent(publicKey);
    if (privateKey == null) {
      return "Login timeout, try again!";
    } else {
      rsaKeyMap.invalidate(publicKey);
    }
    return decryptPassword(response.get(ENCRYPTED_PASSWORD_KEY), privateKey, successFullDecryptionAction);
  }


  @VisibleForTesting
  static String encryptPassword(final String publicKeyString, final String password) {
    try {
      final PublicKey publicKey = KeyFactory.getInstance(RSA)
          .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString)));
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      return Base64.getEncoder().encodeToString(cipher.doFinal(getHashedBytes(password)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns UTF-8 encoded bytes of a "salted" SHA-512 hash of the given input string.
   * The server doesn't need to know the actual password, so this hash essentially replaces
   * the real password. In case any other server authentication system SHA-512 hashes
   * passwords before sending them, we are applying a 'TripleA' prefix to the given String
   * before hashing. This way the hash cannot be used on other websites even if the password
   * and the authentication system is the same.
   */
  private static byte[] getHashedBytes(final String input) {
    return Util.sha512(PSEUDO_SALT + input).getBytes(StandardCharsets.UTF_8);
  }

  /**
   * This method adds the encrypted password (using the specified public key and password)
   * to the specified response map.
   */
  public static Map<String, String> getEncryptedPassword(
      final Map<String, String> challenge,
      final String password) {
    return Collections.singletonMap(ENCRYPTED_PASSWORD_KEY, encryptPassword(challenge.get(RSA_PUBLIC_KEY), password));
  }

  @VisibleForTesting
  static void invalidateAll() {
    rsaKeyMap.invalidateAll();
  }
}
