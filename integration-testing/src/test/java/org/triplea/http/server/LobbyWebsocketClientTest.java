package org.triplea.http.server;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.chat.LobbyChatClient;
import org.triplea.server.http.DropwizardTest;

class LobbyWebsocketClientTest extends DropwizardTest {

  @Test
  @DisplayName("Verify basic websocket operations: open, send, close")
  void verifyConnectivity() throws Exception {
    final URI websocketUri = URI.create(localhost + LobbyChatClient.WEBSOCKET_PATH);

    final WebSocketClient client =
        new WebSocketClient(websocketUri) {
          @Override
          public void onOpen(final ServerHandshake serverHandshake) {}

          @Override
          public void onMessage(final String message) {}

          @Override
          public void onClose(final int code, final String reason, final boolean remote) {}

          @Override
          public void onError(final Exception ex) {}
        };

    assertThat(client.isOpen(), is(false));
    client.connect();

    await().pollDelay(10, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.SECONDS).until(client::isOpen);
    client.send("sending! Just to make sure there are no exception here.");

    // small wait to process any responses
    Thread.sleep(10);

    client.close();
    await()
        .pollDelay(10, TimeUnit.MILLISECONDS)
        .atMost(100, TimeUnit.MILLISECONDS)
        .until(() -> !client.isOpen());
  }
}
