package games.strategy.engine.lobby.server.login;

import java.time.Instant;

import games.strategy.engine.lobby.server.User;

interface AccessLog {
  void logFailedAccess(Instant instant, User user, AuthenticationType authenticationType, String errorMessage);

  void logSuccessfulAccess(Instant instant, User user, AuthenticationType authenticationType);
}
