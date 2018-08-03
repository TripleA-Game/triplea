package games.strategy.engine.lobby.server;

import java.io.IOException;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.util.Version;

public final class LobbyServer {
  public static final String ADMIN_USERNAME = "Admin";
  public static final String LOBBY_CHAT = "_LOBBY_CHAT";
  public static final Version LOBBY_VERSION = new Version(1, 0, 0);

  private LobbyServer() {}

  static void start(final LobbyPropertyReader lobbyPropertyReader) throws IOException {
    final IServerMessenger server = ServerMessenger.newInstanceForLobby(ADMIN_USERNAME, lobbyPropertyReader);
    final Messengers messengers = new Messengers(server);
    server.setLoginValidator(new LobbyLoginValidator(lobbyPropertyReader));
    // setup common objects
    new UserManager(lobbyPropertyReader).register(messengers.getRemoteMessenger());
    final ModeratorController moderatorController = new ModeratorController(server, messengers, lobbyPropertyReader);
    moderatorController.register(messengers.getRemoteMessenger());
    new ChatController(LOBBY_CHAT, messengers, moderatorController);

    // register the status controller
    new StatusManager(messengers).shutDown();

    final LobbyGameController controller = new LobbyGameController((ILobbyGameBroadcaster) messengers
        .getChannelMessenger().getChannelBroadcastor(ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL), server);
    controller.register(messengers.getRemoteMessenger());

    // now we are open for business
    server.setAcceptNewConnections(true);
  }
}
