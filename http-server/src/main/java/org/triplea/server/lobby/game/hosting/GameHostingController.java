package org.triplea.server.lobby.game.hosting;

import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.game.hosting.GameHostingClient;
import org.triplea.http.client.lobby.game.hosting.GameHostingResponse;
import org.triplea.server.http.HttpController;

/**
 * Provides an endpoint where an independent connection can be established, provides an API key to
 * unauthenticated users that can then be used to post a game. Banning rules are verified to ensure
 * banned users cannot post games.
 */
@Builder
public class GameHostingController extends HttpController {

  @Nonnull private final Function<InetAddress, ApiKey> apiKeySupplier;

  @POST
  @Path(GameHostingClient.GAME_HOSTING_REQUEST_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.MINUTES)})
  public GameHostingResponse hostingRequest(@Context final HttpServletRequest request) {
    // TODO: Project#12 check that IP address is allowed to host
    try {
      return GameHostingResponse.builder()
          .apiKey(apiKeySupplier.apply(InetAddress.getByName(request.getRemoteAddr())).getValue())
          .publicVisibleIp(request.getRemoteAddr())
          .build();
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException(
          "Invalid IP address in request: " + request.getRemoteAddr(), e);
    }
  }
}
