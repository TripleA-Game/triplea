package org.triplea.modules.game.listing;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.lobby.game.listing.messages.GameListingMessageFactory;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.web.socket.SessionSet;

/** Receives game listing events and dispatches event messages to listeners. */
@Builder
@RequiredArgsConstructor
public class GameListingEventQueue {
  private final BiConsumer<Collection<Session>, ServerMessageEnvelope> broadcaster;

  @Getter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  private final SessionSet sessionSet;

  public void addListener(final Session session) {
    sessionSet.put(session);
  }

  public void removeListener(final Session session) {
    sessionSet.remove(session);
  }

  void gameRemoved(final String gameId) {
    broadcaster.accept(sessionSet.values(), GameListingMessageFactory.gameRemoved(gameId));
  }

  void gameUpdated(final LobbyGameListing gameListing) {
    broadcaster.accept(sessionSet.values(), GameListingMessageFactory.gameUpdated(gameListing));
  }
}
