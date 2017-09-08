package games.strategy.engine.random;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.vault.Vault;
import games.strategy.engine.vault.VaultID;

/**
 * A random source that generates numbers using a secure algorithm shared
 * between two players.
 * Code originally contributed by Ben Giddings.
 */
public class CryptoRandomSource implements IRandomSource {
  private final IRandomSource m_plainRandom = new PlainRandomSource();

  /**
   * converts an int[] to a byte[].
   */
  public static byte[] intsToBytes(final int[] ints) {
    final byte[] bytes = new byte[ints.length * 4];
    for (int i = 0; i < ints.length; i++) {
      bytes[4 * i] = (byte) (0x000000FF & ints[i]);
      bytes[(4 * i) + 1] = (byte) ((0x000000FF & (ints[i] >> 8)));
      bytes[(4 * i) + 2] = (byte) ((0x000000FF & (ints[i] >> 16)));
      bytes[(4 * i) + 3] = (byte) ((0x000000FF & (ints[i] >> 24)));
    }
    return bytes;
  }

  static int byteToIntUnsigned(final byte val) {
    return val & 0xff;
  }

  static int[] bytesToInts(final byte[] bytes) {
    final int[] ints = new int[bytes.length / 4];
    for (int i = 0; i < ints.length; i++) {
      ints[i] = byteToIntUnsigned(bytes[4 * i]) + (byteToIntUnsigned(bytes[4 * i + 1]) << 8)
          + (byteToIntUnsigned(bytes[4 * i + 2]) << 16) + (byteToIntUnsigned(bytes[4 * i + 3]) << 24);
    }
    return ints;
  }

  static int[] xor(final int[] val1, final int[] val2, final int max) {
    if (val1.length != val2.length) {
      throw new IllegalArgumentException("Arrays not of same length");
    }
    final int[] xorValues = new int[val1.length];
    for (int i = 0; i < val1.length; i++) {
      xorValues[i] = (val1[i] + val2[i]) % max;
    }
    return xorValues;
  }

  // the remote players who involved in rolling the dice
  // dice are rolled securly between us and her
  private final PlayerID m_remotePlayer;
  private final IGame m_game;

  public CryptoRandomSource(final PlayerID remotePlayer, final IGame game) {
    m_remotePlayer = remotePlayer;
    m_game = game;
  }

  /**
   * All delegates should use random data that comes from both players so that
   * neither player cheats.
   */
  @Override
  public int getRandom(final int max, final String annotation) throws IllegalArgumentException, IllegalStateException {
    return getRandom(max, 1, annotation)[0];
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */
  @Override
  public int[] getRandom(final int max, final int count, final String annotation)
      throws IllegalArgumentException, IllegalStateException {
    if (count <= 0) {
      throw new IllegalArgumentException("Invalid count:" + count);
    }
    final Vault vault = m_game.getVault();
    // generate numbers locally, and put them in the vault
    final int[] localRandom = m_plainRandom.getRandom(max, count, annotation);
    // lock it so the client knows that its there, but cant read it
    final VaultID localId = vault.lock(intsToBytes(localRandom));
    // ask the remote to generate numbers
    final IRemoteRandom remote =
        (IRemoteRandom) (m_game.getRemoteMessenger().getRemote(ServerGame.getRemoteRandomName(m_remotePlayer)));
    final Object clientRandom = remote.generate(max, count, annotation, localId);
    if (!(clientRandom instanceof int[])) {
      // Let the error be thrown
      System.out.println("Client remote random generated: " + clientRandom + ".  Asked for: " + count + "x" + max
          + " for " + annotation);
    }
    final int[] remoteNumbers = (int[]) clientRandom;
    // unlock ours, tell the client he can verify
    vault.unlock(localId);
    remote.verifyNumbers();
    // finally, we join the two together to get the real value
    return xor(localRandom, remoteNumbers, max);
  }
}
