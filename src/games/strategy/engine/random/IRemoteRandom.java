package games.strategy.engine.random;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.vault.VaultID;

public interface IRemoteRandom extends IRemote {
  /**
   * Generate a random number, and lock it in the vault.
   *
   * @param serverVaultID
   *        - the vaultID where the server has stored his numbers
   * @return the vault id for which we have locked the data
   */
  public int[] generate(int max, int count, String annotation, VaultID serverVaultID);

  /**
   * unlock the random number last generated.
   */
  public void verifyNumbers();
}
