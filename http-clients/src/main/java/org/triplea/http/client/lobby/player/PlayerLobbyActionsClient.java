package org.triplea.http.client.lobby.player;

import java.net.URI;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.moderator.PlayerSummary;

/** Http client for generic actions that can be executed by a player in lobby. */
public class PlayerLobbyActionsClient {
  public static final String FETCH_PLAYER_INFORMATION = "/lobby/moderator/fetch-player-info";

  private final AuthenticationHeaders authenticationHeaders;
  private final PlayerLobbyActionsFeignClient playerLobbyActionsFeignClient;

  public PlayerLobbyActionsClient(final URI lobbyUri, final ApiKey apiKey) {
    authenticationHeaders = new AuthenticationHeaders(apiKey);
    playerLobbyActionsFeignClient = new HttpClient<>(PlayerLobbyActionsFeignClient.class, lobbyUri).get();
  }

  public PlayerSummary fetchPlayerInformation(final PlayerChatId playerChatId) {
    return playerLobbyActionsFeignClient.fetchPlayerInformation(
        authenticationHeaders.createHeaders(), playerChatId.getValue());
  }
}
