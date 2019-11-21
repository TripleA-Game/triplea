package org.triplea.http.client.lobby.moderator;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ModeratorChatClientTest extends WireMockTest {
  private static final BanPlayerRequest BAN_PLAYER_REQUEST =
      BanPlayerRequest.builder()
          .playerChatId(PlayerChatId.of("chat-id").getValue())
          .banMinutes(20)
          .build();
  private static final PlayerChatId PLAYER_CHAT_ID = PlayerChatId.of("player-chat-id");

  private static ModeratorChatClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ModeratorChatClient::newClient);
  }

  @Test
  void banPlayer(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(ModeratorChatClient.BAN_PLAYER_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalToJson(toJson(BAN_PLAYER_REQUEST)))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).banPlayer(BAN_PLAYER_REQUEST);
  }

  @Test
  void disconnectPlayer(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(ModeratorChatClient.DISCONNECT_PLAYER_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(PLAYER_CHAT_ID.getValue()))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).disconnectPlayer(PLAYER_CHAT_ID);
  }
}
