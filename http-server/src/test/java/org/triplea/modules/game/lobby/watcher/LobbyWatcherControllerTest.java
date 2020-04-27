package org.triplea.modules.game.lobby.watcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.triplea.domain.data.LobbyGame;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatUploadParams;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyWatcherClient;
import org.triplea.modules.TestData;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class LobbyWatcherControllerTest extends ProtectedEndpointTest<LobbyWatcherClient> {

  private static final LobbyGame LOBBY_GAME = TestData.LOBBY_GAME;

  LobbyWatcherControllerTest() {
    super(AllowedUserRole.HOST, LobbyWatcherClient::newClient);
  }

  @Test
  void postGame() {
    verifyEndpoint(client -> client.postGame(LOBBY_GAME));
  }

  @Test
  void removeGame() {
    final String gameId = verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
    verifyEndpoint(client -> client.removeGame(gameId));
  }

  @Test
  void keepAlive() {
    final String gameId = verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
    final boolean result = verifyEndpointReturningObject(client -> client.sendKeepAlive(gameId));
    assertThat(result, is(true));
  }

  @Test
  void updateGame() {
    final String gameId = verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));
    verifyEndpoint(client -> client.updateGame(gameId, LOBBY_GAME));
  }

  @Test
  void uploadChat() {
    final String gameId = verifyEndpointReturningObject(client -> client.postGame(LOBBY_GAME));

    verifyEndpoint(
        client ->
            client.uploadChatMessage(
                AllowedUserRole.HOST.getAllowedKey(),
                ChatUploadParams.builder()
                    .fromPlayer(UserName.of("player"))
                    .chatMessage("chat")
                    .gameId(gameId)
                    .build()));
  }
}
