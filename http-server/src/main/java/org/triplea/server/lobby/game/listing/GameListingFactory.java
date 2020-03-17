package org.triplea.server.lobby.game.listing;

import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.java.cache.ExpiringAfterWriteCache;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;

@UtilityClass
public final class GameListingFactory {
  public static GameListing buildGameListing(
      final Jdbi jdbi, final GameListingEventQueue gameListingEventQueue) {
    return GameListing.builder()
        .auditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .gameListingEventQueue(gameListingEventQueue)
        .games(
            new ExpiringAfterWriteCache<>(
                GameListingClient.KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new GameTtlExpiredListener(gameListingEventQueue)))
        .build();
  }
}
