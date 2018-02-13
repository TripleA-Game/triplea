package games.strategy.engine.lobby.server.login;

import java.sql.SQLException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.db.AccessLogController;
import games.strategy.engine.lobby.server.db.AccessLogDao;

/**
 * Implementation of {@link AccessLog} that logs lobby access attempts to both the system logger framework and the
 * database access log table.
 */
final class CompositeAccessLog implements AccessLog {
  private static final Logger logger = Logger.getLogger(CompositeAccessLog.class.getName());

  private final AccessLogDao accessLogDao;

  CompositeAccessLog() {
    this(new AccessLogController());
  }

  @VisibleForTesting
  CompositeAccessLog(final AccessLogDao accessLogDao) {
    this.accessLogDao = accessLogDao;
  }

  @Override
  public void logFailedAuthentication(
      final Instant instant,
      final User user,
      final UserType userType,
      final String errorMessage) {
    logger.info(String.format("Failed authentication by %s user at [%s]: name: %s, IP: %s, MAC: %s, error: %s",
        userType.toString().toLowerCase(),
        instant,
        user.getUsername(),
        user.getInetAddress().getHostAddress(),
        user.getHashedMacAddress(),
        errorMessage));
  }

  @Override
  public void logSuccessfulAuthentication(final Instant instant, final User user, final UserType userType) {
    logger.info(String.format("Successful authentication by %s user at [%s]: name: %s, IP: %s, MAC: %s",
        userType.toString().toLowerCase(),
        instant,
        user.getUsername(),
        user.getInetAddress().getHostAddress(),
        user.getHashedMacAddress()));

    try {
      accessLogDao.insert(instant, user, userType);
    } catch (final SQLException e) {
      logger.log(Level.SEVERE, "failed to record successful authentication in database", e);
    }
  }
}
