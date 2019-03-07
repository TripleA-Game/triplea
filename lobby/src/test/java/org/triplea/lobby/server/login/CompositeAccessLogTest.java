package org.triplea.lobby.server.login;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.TestUserUtils;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.AccessLogDao;

@ExtendWith(MockitoExtension.class) final class CompositeAccessLogTest {
  @Mock
  private AccessLogDao accessLogDao;

  @InjectMocks
  private CompositeAccessLog compositeAccessLog;

  private final User user = TestUserUtils.newUser();

  @Test void logFailedAuthentication_ShouldNotAddDatabaseAccessLogRecord() throws Exception {
    for (final UserType userType : UserType.values()) {
      compositeAccessLog.logFailedAuthentication(user, userType, "error message");

      verify(accessLogDao, never()).insert(any(User.class), any(UserType.class));
    }
  }

  @Test void logSuccessfulAuthentication_ShouldAddDatabaseAccessLogRecord() throws Exception {
    for (final UserType userType : UserType.values()) {
      compositeAccessLog.logSuccessfulAuthentication(user, userType);

      verify(accessLogDao).insert(user, userType);
    }
  }
}
