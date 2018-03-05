package games.strategy.engine.lobby.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.sound.ClipPlayer;

public class LobbyRunner {
  private static final Logger logger = Logger.getLogger(LobbyServer.class.getName());

  /**
   * Launches a lobby instance.
   * Lobby stays running until the process is killed or the lobby is shutdown.
   */
  public static void main(final String[] args) {
    try {
      ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
      final int port = LobbyContext.lobbyPropertyReader().getPort();
      logger.info("Trying to listen on port:" + port);
      new LobbyServer(port);
      logger.info("Lobby started");
    } catch (final Exception ex) {
      logger.log(Level.SEVERE, ex.toString(), ex);
    }
  }
}
