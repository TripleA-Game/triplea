package org.triplea.server.user.account.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.domain.data.SystemId;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.LoginRequest;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.lobby.server.db.data.UserRole;

@ExtendWith(MockitoExtension.class)
class LoginModuleTest {

  private static final SystemId SYSTEM_ID = SystemId.of("system-id");
  private static final String IP = "ip";

  private static final LoginRequest LOGIN_REQUEST =
      LoginRequest.builder().name("name").password("password").build();

  private static final LoginRequest ANONYMOUS_LOGIN_REQUEST =
      LoginRequest.builder().name("name").build();

  private static final ApiKey API_KEY = ApiKey.of("api-key");

  @Mock private Predicate<LoginRequest> registeredLogin;
  @Mock private Predicate<LoginRequest> tempPasswordLogin;
  @Mock private Function<PlayerName, Optional<String>> anonymousLogin;
  @Mock private Function<LoginRecord, ApiKey> apiKeyGenerator;
  @Mock private UserJdbiDao userJdbiDao;

  private LoginModule loginModule;

  @BeforeEach
  void setup() {
    loginModule =
        LoginModule.builder()
            .registeredLogin(registeredLogin)
            .tempPasswordLogin(tempPasswordLogin)
            .anonymousLogin(anonymousLogin)
            .apiKeyGenerator(apiKeyGenerator)
            .userJdbiDao(userJdbiDao)
            .build();
  }

  @SuppressWarnings("unused")
  static List<Arguments> rejectLoginOnBadArgs() {
    return List.of(
        Arguments.of(LoginRequest.builder().password("no-name").build(), "system-id-string", IP),
        Arguments.of(LOGIN_REQUEST, null, IP));
  }

  @ParameterizedTest
  @MethodSource
  void rejectLoginOnBadArgs(
      final LoginRequest loginRequest, final String systemIdString, final String ip) {
    final LobbyLoginResponse result = loginModule.doLogin(loginRequest, systemIdString, ip);

    assertFailedLogin(result);
  }

  private void assertFailedLogin(final LobbyLoginResponse result) {
    assertThat(result.getFailReason(), notNullValue());
    assertThat(result.getApiKey(), nullValue());
    verify(userJdbiDao, never()).lookupUserRoleByUserName(any());
    verify(apiKeyGenerator, never()).apply(any());
  }

  private static void assertSuccessLogin(final LobbyLoginResponse result) {
    assertThat(result.getFailReason(), nullValue());
    assertThat(result.getApiKey(), is(API_KEY.getValue()));
  }

  private void givenApiKeyGenerationForAnonymous() {
    // TODO: Project#12 verify anonymous record recorded
    when(apiKeyGenerator.apply(any(LoginRecord.class))).thenReturn(API_KEY);
  }

  private void givenApiKeyGenerationForRegistered() {
    // TODO: Project#12 verify registered user record recorded
    when(apiKeyGenerator.apply(any(LoginRecord.class))).thenReturn(API_KEY);
  }

  @Nested
  class AnonymousLogin {
    @Test
    void loginRejected() {
      when(anonymousLogin.apply(PlayerName.of(ANONYMOUS_LOGIN_REQUEST.getName())))
          .thenReturn(Optional.of("error"));

      final LobbyLoginResponse result =
          loginModule.doLogin(ANONYMOUS_LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertFailedLogin(result);
      verify(registeredLogin, never()).test(any());
      verify(tempPasswordLogin, never()).test(any());
    }

    @Test
    void loginSuccess() {
      when(anonymousLogin.apply(PlayerName.of(ANONYMOUS_LOGIN_REQUEST.getName())))
          .thenReturn(Optional.empty());
      givenApiKeyGenerationForAnonymous();

      final LobbyLoginResponse result =
          loginModule.doLogin(ANONYMOUS_LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertSuccessLogin(result);
      assertThat(result.isPasswordChangeRequired(), is(false));
      verify(registeredLogin, never()).test(any());
      verify(tempPasswordLogin, never()).test(any());
      verify(userJdbiDao, never()).lookupUserRoleByUserName(any());
    }
  }

  @Nested
  class RegisteredLogin {
    @Test
    void loginRejected() {
      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertFailedLogin(result);

      verify(anonymousLogin, never()).apply(any());
    }

    @Test
    void loginSuccess() {
      when(registeredLogin.test(LOGIN_REQUEST)).thenReturn(true);
      when(userJdbiDao.lookupUserRoleByUserName(LOGIN_REQUEST.getName()))
          .thenReturn(Optional.of(UserRole.PLAYER));
      givenApiKeyGenerationForRegistered();

      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertSuccessLogin(result);
      assertThat(result.isPasswordChangeRequired(), is(false));
      verify(anonymousLogin, never()).apply(any());
    }
  }

  @Nested
  class TempPasswordLogin {
    @Test
    void loginSuccess() {
      when(tempPasswordLogin.test(LOGIN_REQUEST)).thenReturn(true);
      when(userJdbiDao.lookupUserRoleByUserName(LOGIN_REQUEST.getName()))
          .thenReturn(Optional.of(UserRole.PLAYER));
      givenApiKeyGenerationForRegistered();

      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertSuccessLogin(result);
      assertThat(result.isPasswordChangeRequired(), is(true));
      verify(anonymousLogin, never()).apply(any());
    }
  }

  /** Verify lobby login result 'isModerator' flag has expected values. */
  @Nested
  class ModeratorUser {
    @ParameterizedTest
    @ValueSource(strings = {UserRole.MODERATOR, UserRole.ADMIN})
    void loginSuccessWithModerator(final String moderatorUserRole) {
      givenLoginWithUserRole(moderatorUserRole);

      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertThat(result.isModerator(), is(true));
    }

    private void givenLoginWithUserRole(final String userRole) {
      when(registeredLogin.test(LOGIN_REQUEST)).thenReturn(true);
      when(userJdbiDao.lookupUserRoleByUserName(LOGIN_REQUEST.getName()))
          .thenReturn(Optional.of(userRole));
      givenApiKeyGenerationForRegistered();
    }

    @ParameterizedTest
    @ValueSource(strings = {UserRole.ANONYMOUS, UserRole.PLAYER})
    void loginSuccessWithNonModerator(final String nonModeratorUserRole) {
      givenLoginWithUserRole(nonModeratorUserRole);

      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertThat(result.isModerator(), is(false));
    }
  }
}
