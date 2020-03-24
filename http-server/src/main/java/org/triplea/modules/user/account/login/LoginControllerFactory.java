package org.triplea.modules.user.account.login;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.TempPasswordDao;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.db.dao.access.log.AccessLogDao;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.modules.chat.event.processing.Chatters;
import org.triplea.modules.user.account.login.authorizer.BCryptHashVerifier;
import org.triplea.modules.user.account.login.authorizer.anonymous.AnonymousLoginFactory;
import org.triplea.modules.user.account.login.authorizer.registered.RegisteredLoginFactory;
import org.triplea.modules.user.account.login.authorizer.temp.password.TempPasswordLogin;

@UtilityClass
public class LoginControllerFactory {

  public static LoginController buildController(final Jdbi jdbi, final Chatters chatters) {
    final UserJdbiDao userJdbiDao = jdbi.onDemand(UserJdbiDao.class);

    return LoginController.builder()
        .loginModule(
            LoginModule.builder()
                .userJdbiDao(userJdbiDao)
                .accessLogUpdater(
                    AccessLogUpdater.builder()
                        .accessLogDao(jdbi.onDemand(AccessLogDao.class))
                        .build())
                .apiKeyGenerator(
                    ApiKeyGenerator.builder()
                        .apiKeyDaoWrapper(ApiKeyDaoWrapper.build(jdbi))
                        .build())
                .anonymousLogin(AnonymousLoginFactory.build(jdbi, chatters))
                .tempPasswordLogin(
                    TempPasswordLogin.builder()
                        .tempPasswordDao(jdbi.onDemand(TempPasswordDao.class))
                        .passwordChecker(new BCryptHashVerifier())
                        .build())
                .registeredLogin(RegisteredLoginFactory.build(jdbi))
                .build())
        .build();
  }
}
