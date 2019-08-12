package org.triplea.lobby.server;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.net.DefaultObjectStreamFactory;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.ILobbyGameBroadcaster;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.lobby.server.login.FailedLoginThrottle;
import org.triplea.lobby.server.login.LobbyLoginValidator;

/**
 * A lobby server.
 *
 * <p>A lobby server provides the following functionality:
 *
 * <ul>
 *   <li>A registry of servers available to host games.
 *   <li>A room where players can find opponents and generally chat.
 * </ul>
 */
final class LobbyServer {
  private LobbyServer() {}

  /**
   * Starts a new lobby server using the properties given by {@code lobbyConfiguration}.
   *
   * <p>This method returns immediately after the lobby server is started; it does not block while
   * the lobby server is running.
   */
  static void start(final LobbyConfiguration lobbyConfiguration) throws IOException {
    ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);

    final IServerMessenger server =
        new ServerMessenger(
            LobbyConstants.ADMIN_USERNAME,
            lobbyConfiguration.getPort(),
            new DefaultObjectStreamFactory());
    final Messengers messengers = new Messengers(server);
    server.setLoginValidator(
        new LobbyLoginValidator(
            lobbyConfiguration.getDatabaseDao(),
            new RsaAuthenticator(),
            BCrypt::gensalt,
            new FailedLoginThrottle()));

    new UserManager(lobbyConfiguration.getDatabaseDao()).register(messengers);

    final ModeratorController moderatorController =
        new ModeratorController(server, messengers, lobbyConfiguration.getDatabaseDao());
    moderatorController.register(messengers);
    new ChatController(LobbyConstants.LOBBY_CHAT, messengers, moderatorController::isPlayerAdmin);

    // register the status controller
    new StatusManager(messengers).shutDown();

    final LobbyGameController controller =
        new LobbyGameController(
            (ILobbyGameBroadcaster)
                messengers.getChannelBroadcaster(ILobbyGameBroadcaster.REMOTE_NAME),
            server);
    controller.register(messengers);

    // now we are open for business
    server.setAcceptNewConnections(true);
  }
}
