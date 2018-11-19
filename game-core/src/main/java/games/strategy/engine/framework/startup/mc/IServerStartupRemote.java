package games.strategy.engine.framework.startup.mc;

import java.util.Set;

import games.strategy.engine.framework.HeadlessAutoSaveType;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;

/**
 * Allows client nodes to access various information from the server node during network game setup.
 */
public interface IServerStartupRemote extends IRemote {
  /**
   * Returns a listing of the players in the game.
   */
  PlayerListing getPlayerListing();

  void takePlayer(INode who, String playerName);

  void releasePlayer(INode who, String playerName);

  void disablePlayer(String playerName);

  void enablePlayer(String playerName);

  /**
   * Has the game already started?
   * If true, the server will call our ObserverWaitingToJoin to start the game.
   * Note, the return value may come back after our ObserverWaitingToJoin has been created
   */
  boolean isGameStarted(INode newNode);

  boolean getIsServerHeadless();

  Set<String> getAvailableGames();

  void changeServerGameTo(final String gameName);

  void changeToLatestAutosave(final HeadlessAutoSaveType typeOfAutosave);

  void changeToGameSave(final byte[] bytes, final String fileName);

  byte[] getSaveGame();

  byte[] getGameOptions();

  void changeToGameOptions(final byte[] bytes);
}
