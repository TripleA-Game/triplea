package games.strategy.engine.random;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * A service that provides dice rolling facilities outside the game process. Used to provide a level of trust between
 * players that no one player is cheating using a compromised local dice server.
 */
public interface IRemoteDiceServer {

  String NAME = "DICE_SERVER_NAME";
  String GAME_NAME = "DICE_SERVER_GAME_NAME";
  String EMAIL_1 = "DICE_SERVER_EMAIL_1";
  String EMAIL_2 = "DICE_SERVER_EMAIL_2";
  /**
   * Post a request to the dice server, and return the resulting html page as a string.
   */
  String postRequest(int max, int numDice, String subjectMessage, String gameId) throws IOException;

  /**
   * Given the html page returned from postRequest, return the dice []
   * throw an InvocationTargetException to indicate an error message to be returned.
   *
   * @throws IOException If there was an error parsing the string.
   */
  int[] getDice(String string, int count) throws IOException, InvocationTargetException;

  /**
   * Get the to address.
   *
   * @return returns the to address or null if not configured
   */
  String getToAddress();

  /**
   * get the CC address.
   *
   * @return the address or null if not configured
   */
  String getCcAddress();

  /**
   * Get the configured game id.
   *
   * @return the game id or null if not configured
   */
  String getGameId();

  String getDisplayName();
}
