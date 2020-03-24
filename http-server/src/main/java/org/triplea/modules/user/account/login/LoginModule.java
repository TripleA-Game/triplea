package org.triplea.modules.user.account.login;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.LoginRequest;

@Builder
class LoginModule {
  @Nonnull private Predicate<LoginRequest> registeredLogin;
  @Nonnull private Predicate<LoginRequest> tempPasswordLogin;
  @Nonnull private Function<UserName, Optional<String>> anonymousLogin;
  @Nonnull private Consumer<LoginRecord> accessLogUpdater;
  @Nonnull private final Function<LoginRecord, ApiKey> apiKeyGenerator;
  @Nonnull private final UserJdbiDao userJdbiDao;

  public LobbyLoginResponse doLogin(
      final LoginRequest loginRequest, final String systemId, final String ip) {
    if (loginRequest == null
        || Strings.nullToEmpty(loginRequest.getName()).isEmpty()
        || Strings.nullToEmpty(systemId).isEmpty()) {
      return LobbyLoginResponse.builder().failReason("Invalid login request").build();
    }

    final SystemId playerSystemId = SystemId.of(systemId);
    final String nameValidation = UserName.validate(loginRequest.getName());
    if (nameValidation != null) {
      return LobbyLoginResponse.builder().failReason("Invalid name: " + nameValidation).build();
    }

    final boolean hasPassword = !Strings.nullToEmpty(loginRequest.getPassword()).isEmpty();

    if (hasPassword && registeredLogin.test(loginRequest)) {
      final ApiKey apiKey =
          recordRegisteredLoginAndGenerateApiKey(
              loginRequest, playerSystemId, PlayerChatId.newId(), ip);
      return LobbyLoginResponse.builder()
          .apiKey(apiKey.getValue())
          .moderator(isModerator(loginRequest.getName()))
          .build();
    } else if (hasPassword && tempPasswordLogin.test(loginRequest)) {
      final ApiKey apiKey =
          recordRegisteredLoginAndGenerateApiKey(
              loginRequest, playerSystemId, PlayerChatId.newId(), ip);
      return LobbyLoginResponse.builder()
          .apiKey(apiKey.getValue())
          .passwordChangeRequired(true)
          .moderator(isModerator(loginRequest.getName()))
          .build();
    } else if (hasPassword) {
      return LobbyLoginResponse.builder()
          .failReason("Invalid username and password combination")
          .build();
    } else { // anonymous login
      final Optional<String> errorMessage =
          anonymousLogin.apply(UserName.of(loginRequest.getName()));
      if (errorMessage.isPresent()) {
        return LobbyLoginResponse.builder().failReason(errorMessage.get()).build();
      } else {
        final ApiKey apiKey =
            recordAnonymousLoginAndGenerateApiKey(
                loginRequest, playerSystemId, PlayerChatId.newId(), ip);
        return LobbyLoginResponse.builder().apiKey(apiKey.getValue()).build();
      }
    }
  }

  private ApiKey recordRegisteredLoginAndGenerateApiKey(
      final LoginRequest loginRequest,
      final SystemId systemId,
      final PlayerChatId playerChatId,
      final String ip) {
    return recordLoginAndGenerateApiKey(loginRequest, systemId, playerChatId, ip, true);
  }

  private ApiKey recordAnonymousLoginAndGenerateApiKey(
      final LoginRequest loginRequest,
      final SystemId systemId,
      final PlayerChatId playerChatId,
      final String ip) {
    return recordLoginAndGenerateApiKey(loginRequest, systemId, playerChatId, ip, false);
  }

  // TODO: Project#12 also update access log
  @SuppressWarnings("unused")
  private ApiKey recordLoginAndGenerateApiKey(
      final LoginRequest loginRequest,
      final SystemId systemId,
      final PlayerChatId playerchatId,
      final String ip,
      final boolean isRegistered) {
    final var loginRecord =
        LoginRecord.builder()
            .userName(UserName.of(loginRequest.getName()))
            .systemId(systemId)
            .playerChatId(playerchatId)
            .ip(ip)
            .registered(isRegistered)
            .build();
    accessLogUpdater.accept(loginRecord);
    return apiKeyGenerator.apply(loginRecord);
  }

  private boolean isModerator(final String username) {
    return userJdbiDao
        .lookupUserRoleByUserName(username)
        .map(UserRole::isModerator)
        .orElseThrow(() -> new AssertionError("Expected to find role for user: " + username));
  }
}
