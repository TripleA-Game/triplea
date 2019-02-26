package org.triplea.lobby.server.login;

import java.sql.SQLException;
import java.util.logging.Level;

import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.AccessLogDao;

import lombok.extern.java.Log;

/**
 * Implementation of {@link AccessLog} that logs lobby access attempts to both the system logger framework and the
 * database access log table.
 */
@Log
final class CompositeAccessLog implements AccessLog {
  private final AccessLogDao accessLogDao;

  CompositeAccessLog(final AccessLogDao accessLogDao) {
    this.accessLogDao = accessLogDao;
  }

  @Override
  public void logFailedAuthentication(final User user, final UserType userType, final String errorMessage) {
    log.info(String.format("Failed authentication by %s user: name: %s, IP: %s, MAC: %s, error: %s",
        userType.toString().toLowerCase(),
        user.getUsername(),
        user.getInetAddress().getHostAddress(),
        user.getHashedMacAddress(),
        errorMessage));
  }

  @Override
  public void logSuccessfulAuthentication(final User user, final UserType userType) {
    log.info(String.format("Successful authentication by %s user: name: %s, IP: %s, MAC: %s",
        userType.toString().toLowerCase(),
        user.getUsername(),
        user.getInetAddress().getHostAddress(),
        user.getHashedMacAddress()));

    try {
      accessLogDao.insert(user, userType);
    } catch (final SQLException e) {
      log.log(Level.SEVERE, "failed to record successful authentication in database", e);
    }
  }
}
